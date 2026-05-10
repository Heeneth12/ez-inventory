package com.ezh.Inventory.sales.returns.service;

import com.ezh.Inventory.approval.dto.ApprovalCheckContext;
import com.ezh.Inventory.approval.entity.ApprovalResultStatus;
import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import com.ezh.Inventory.approval.service.ApprovalService;
import com.ezh.Inventory.payment.service.CreditNoteService;
import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoiceItem;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.payment.service.PaymentService;
import com.ezh.Inventory.sales.returns.dto.ReturnItemRequest;
import com.ezh.Inventory.sales.returns.dto.SalesReturnDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnFilter;
import com.ezh.Inventory.sales.returns.dto.SalesReturnItemDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnRequestDto;
import com.ezh.Inventory.sales.returns.entity.SalesReturn;
import com.ezh.Inventory.sales.returns.entity.SalesReturnItem;
import com.ezh.Inventory.sales.returns.entity.SalesReturnStatus;
import com.ezh.Inventory.sales.returns.repository.SalesReturnRepository;
import com.ezh.Inventory.stock.dto.StockUpdateDto;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import com.ezh.Inventory.stock.entity.StockBatch;
import com.ezh.Inventory.stock.repository.StockBatchRepository;
import com.ezh.Inventory.stock.service.StockService;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.common.events.ApprovalDecisionEvent;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReturnServiceImpl implements SalesReturnService {

    private final InvoiceRepository invoiceRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final StockBatchRepository stockBatchRepository;
    private final AuthServiceClient authServiceClient;
    private final ApprovalService approvalService;
    private final CreditNoteService creditNoteService;

    @Override
    @Transactional
    public CommonResponse<?> createSalesReturn(SalesReturnRequestDto request) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        SalesReturn salesReturn = SalesReturn.builder()
                .tenantId(tenantId)
                .returnNumber(DocumentNumberUtil.generate(DocPrefix.SR))
                .invoice(invoice)
                .returnDate(new Date())
                .totalAmount(BigDecimal.ZERO)
                .status(SalesReturnStatus.PENDING_APPROVAL)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (ReturnItemRequest itemReq : request.getItems()) {
            InvoiceItem originalSoldItem = resolveInvoiceItem(invoice, itemReq);

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new CommonException("Return quantity must be greater than zero.", HttpStatus.BAD_REQUEST);
            }

            int alreadyReturned = originalSoldItem.getReturnedQuantity() != null
                    ? originalSoldItem.getReturnedQuantity()
                    : 0;
            int maximumAllowedToReturn = originalSoldItem.getQuantity() - alreadyReturned;

            if (itemReq.getQuantity() > maximumAllowedToReturn) {
                throw new CommonException("Cannot return " + itemReq.getQuantity() +
                        " of " + originalSoldItem.getItemName() +
                        ". Total bought: " + originalSoldItem.getQuantity() +
                        ", Already returned: " + alreadyReturned +
                        ". You can only return up to " + maximumAllowedToReturn + " more.",
                        HttpStatus.BAD_REQUEST);
            }

            BigDecimal actualPaidPerUnit = originalSoldItem.getUnitPrice();
            BigDecimal lineRefundTotal = actualPaidPerUnit.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalRefundAmount = totalRefundAmount.add(lineRefundTotal);

            SalesReturnItem returnItem = SalesReturnItem.builder()
                    .salesReturn(salesReturn)
                    .itemId(itemReq.getItemId())
                    .batchNumber(originalSoldItem.getBatchNumber())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(actualPaidPerUnit)
                    .reason(request.getReason())
                    .build();

            salesReturn.getItems().add(returnItem);
        }

        salesReturn.setTotalAmount(totalRefundAmount);

        SalesReturn savedReturn = salesReturnRepository.save(salesReturn);

        ApprovalCheckContext approvalCheckContext = ApprovalCheckContext.builder()
                .type(ApprovalType.SALES_RETURN)
                .amount(totalRefundAmount)
                .referenceId(savedReturn.getId())
                .referenceCode(savedReturn.getReturnNumber())
                .build();

        CommonResponse<?> approvalResponse = approvalService.checkAndInitiateApproval(approvalCheckContext);

        if (approvalResponse.getData() == ApprovalResultStatus.APPROVAL_REQUIRED) {
            savedReturn.setStatus(SalesReturnStatus.PENDING_APPROVAL);
            salesReturnRepository.save(savedReturn);

            return CommonResponse.builder()
                    .id(savedReturn.getId().toString())
                    .message("Sales return submitted for approval")
                    .status(Status.SUCCESS)
                    .build();
        }

        applyApprovedSalesReturn(savedReturn);

        return CommonResponse.builder()
                .id(savedReturn.getId().toString())
                .message("Sales return processed successfully")
                .status(Status.SUCCESS)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesReturnDto> getSalesReturns(SalesReturnFilter filter, Integer page, Integer size) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<SalesReturn> data = salesReturnRepository.getAllSalesReturn(
                tenantId,
                filter.getId(),
                filter.getCustomerId(),
                filter.getInvoiceId(),
                filter.getWarehouseId(),
                filter.getStatuses(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        Set<Long> customerIds = data.stream()
                .map(sr -> sr.getInvoice().getCustomerId())
                .collect(Collectors.toSet());

        Map<Long, UserMiniDto> customerMap = customerIds.isEmpty()
                ? new HashMap<>()
                : authServiceClient.getBulkUserDetails(customerIds.stream().toList());

        return data.map(salesReturn -> mapToDto(salesReturn, customerMap));
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReturnDto getSalesReturnById(Long id) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        SalesReturn salesReturn = salesReturnRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Sales Return not found with ID: " + id,
                        HttpStatus.NOT_FOUND));

        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(
                List.of(salesReturn.getInvoice().getCustomerId()));

        return mapToDto(salesReturn, customerMap);
    }

    @EventListener
    @Transactional
    public void onApprovalDecision(ApprovalDecisionEvent event) {
        if (event.getType() != ApprovalType.SALES_RETURN) {
            return;
        }

        SalesReturn salesReturn = salesReturnRepository.findById(event.getReferenceId())
                .orElseThrow(() -> new CommonException("Linked Sales Return not found", HttpStatus.NOT_FOUND));

        if (event.getStatus() == ApprovalStatus.APPROVED) {
            applyApprovedSalesReturn(salesReturn);
        } else {
            salesReturn.setStatus(SalesReturnStatus.REJECTED);
            salesReturnRepository.save(salesReturn);
        }
    }

    private void applyApprovedSalesReturn(SalesReturn salesReturn) {
        if (salesReturn.getStatus() == SalesReturnStatus.APPROVED) {
            return;
        }

        Invoice invoice = salesReturn.getInvoice();

        for (SalesReturnItem returnItem : salesReturn.getItems()) {
            InvoiceItem originalSoldItem = resolveInvoiceItem(invoice,
                    ReturnItemRequest.builder()
                            .itemId(returnItem.getItemId())
                            .batchNumber(returnItem.getBatchNumber())
                            .quantity(returnItem.getQuantity())
                            .build());

            int alreadyReturned = originalSoldItem.getReturnedQuantity() != null
                    ? originalSoldItem.getReturnedQuantity()
                    : 0;
            int maximumAllowedToReturn = originalSoldItem.getQuantity() - alreadyReturned;
            if (returnItem.getQuantity() > maximumAllowedToReturn) {
                throw new CommonException("Cannot approve sales return for item " + originalSoldItem.getItemName() +
                        " because allowed return quantity is now " + maximumAllowedToReturn,
                        HttpStatus.BAD_REQUEST);
            }

            originalSoldItem.setReturnedQuantity(alreadyReturned + returnItem.getQuantity());

            String batchNum = returnItem.getBatchNumber();
            StockBatch batch = stockBatchRepository
                    .findByItemIdAndBatchNumberAndWarehouseId(returnItem.getItemId(), batchNum, invoice.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Original Batch details not found"));

            StockUpdateDto stockDto = StockUpdateDto.builder()
                    .itemId(returnItem.getItemId())
                    .warehouseId(invoice.getWarehouseId())
                    .quantity(returnItem.getQuantity())
                    .transactionType(MovementType.IN)
                    .referenceType(ReferenceType.SALES_RETURN)
                    .referenceId(salesReturn.getId())
                    .remarks("Returned from Invoice " + invoice.getInvoiceNumber())
                    .batchNumber(batchNum)
                    .unitPrice(batch.getBuyPrice())
                    .build();

            stockService.updateStock(stockDto);
        }

        creditNoteService.createCreditNote(
                invoice.getCustomerId(),
                salesReturn.getTotalAmount(),
                salesReturn.getId()
        );
//        cr.createCreditNote(
//                invoice.getCustomerId(),
//                salesReturn.getTotalAmount(),
//                salesReturn.getReturnNumber());

        salesReturn.setStatus(SalesReturnStatus.COMPLETED);
        salesReturnRepository.save(salesReturn);
    }

    private InvoiceItem resolveInvoiceItem(Invoice invoice, ReturnItemRequest itemReq) {
        List<InvoiceItem> matchingItems = invoice.getItems().stream()
                .filter(i -> i.getItemId().equals(itemReq.getItemId()))
                .toList();

        if (matchingItems.isEmpty()) {
            throw new CommonException("Item " + itemReq.getItemId() + " not found in invoice", HttpStatus.NOT_FOUND);
        }

        if (itemReq.getBatchNumber() != null && !itemReq.getBatchNumber().isBlank()) {
            return matchingItems.stream()
                    .filter(i -> itemReq.getBatchNumber().equals(i.getBatchNumber()))
                    .findFirst()
                    .orElseThrow(() -> new CommonException(
                            "Batch " + itemReq.getBatchNumber() + " not found for item in invoice",
                            HttpStatus.NOT_FOUND));
        }

        if (matchingItems.size() == 1) {
            return matchingItems.getFirst();
        }

        throw new CommonException(
                "Multiple batches found for item. Please provide batchNumber.",
                HttpStatus.BAD_REQUEST);
    }

    private SalesReturnDto mapToDto(SalesReturn entity, Map<Long, UserMiniDto> customerMap) {

        if (entity == null)
            return null;

        List<SalesReturnItemDto> items = entity.getItems()
                .stream()
                .map(item -> SalesReturnItemDto.builder()
                        .id(item.getId())
                        .itemId(item.getItemId())
                        .batchNumber(item.getBatchNumber())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .reason(item.getReason())
                        .build())
                .toList();

        UserMiniDto contactMini = customerMap != null
                ? customerMap.getOrDefault(entity.getInvoice().getCustomerId(), new UserMiniDto())
                : null;

        return SalesReturnDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .returnNumber(entity.getReturnNumber())
                .invoiceId(entity.getInvoice().getId())
                .customerId(entity.getInvoice().getCustomerId())
                .contactMini(contactMini)
                .returnDate(entity.getReturnDate())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .items(items)
                .creditNotePaymentId(
                        entity.getCreditNotePayment() != null
                                ? entity.getCreditNotePayment().getId()
                                : null)
                .build();
    }
}
