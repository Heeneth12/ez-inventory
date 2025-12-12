package com.ezh.Inventory.sales.returns.service;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoiceItem;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.sales.payment.service.PaymentService;
import com.ezh.Inventory.sales.returns.dto.ReturnItemRequest;
import com.ezh.Inventory.sales.returns.dto.SalesReturnDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnItemDto;
import com.ezh.Inventory.sales.returns.dto.SalesReturnRequestDto;
import com.ezh.Inventory.sales.returns.entity.SalesReturn;
import com.ezh.Inventory.sales.returns.entity.SalesReturnItem;
import com.ezh.Inventory.sales.returns.repository.SalesReturnRepository;
import com.ezh.Inventory.stock.dto.StockUpdateDto;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import com.ezh.Inventory.stock.entity.StockBatch;
import com.ezh.Inventory.stock.repository.StockBatchRepository;
import com.ezh.Inventory.stock.service.StockService;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReturnServiceImpl implements SalesReturnService {

    private final InvoiceRepository invoiceRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final StockBatchRepository stockBatchRepository;


    @Override
    @Transactional
    public CommonResponse createSalesReturn(SalesReturnRequestDto request) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        SalesReturn salesReturn = SalesReturn.builder()
                .tenantId(tenantId)
                .returnNumber("SR-" + System.currentTimeMillis())
                .invoice(invoice)
                .returnDate(new Date())
                .items(new ArrayList<>())
                .build();

        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (ReturnItemRequest itemReq : request.getItems()) {

            InvoiceItem originalSoldItem = invoice.getItems().stream()
                    .filter(i -> i.getItemId().equals(itemReq.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new CommonException("Item not found in invoice", HttpStatus.NOT_FOUND));

            // --- FIX B: Check for PREVIOUS returns ---
            // You need a helper method or field on InvoiceItem to track 'returnedQty'
            // Assuming InvoiceItem has a field 'returnedQuantity' that you update:
            int alreadyReturned = originalSoldItem.getReturnedQuantity() != null ? originalSoldItem.getReturnedQuantity() : 0;
            int remainingQty = originalSoldItem.getQuantity() - alreadyReturned;

            if (itemReq.getQuantity() > remainingQty) {
                throw new CommonException("Cannot return " + itemReq.getQuantity() +
                        ". Only " + remainingQty + " remaining for this item.", HttpStatus.BAD_REQUEST);
            }

            // Update the InvoiceItem so they can't return it again next time
            originalSoldItem.setReturnedQuantity(alreadyReturned + itemReq.getQuantity());
            // invoiceItemRepository.save(originalSoldItem); // If not cascading

            // --- BATCH LOGIC ---
            String batchNum = originalSoldItem.getBatchNumber();
            StockBatch batch = stockBatchRepository
                    .findByItemIdAndBatchNumberAndWarehouseId(itemReq.getItemId(), batchNum, invoice.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Original Batch details not found"));

            BigDecimal originalBuyPrice = batch.getBuyPrice();

            // --- FIX C: Price Logic ---
            // Assuming InvoiceItem.unitPrice is truly the "Price per unit"
            // If discount was applied to the whole line, calculate actual paid per unit:
            // BigDecimal actualPaidPerUnit = originalSoldItem.getTotalAmount().divide(BigDecimal.valueOf(originalSoldItem.getQuantity()), 2, RoundingMode.HALF_UP);

            // If unitPrice is just the price, use it directly:
            BigDecimal actualPaidPerUnit = originalSoldItem.getUnitPrice();

            BigDecimal lineRefundTotal = actualPaidPerUnit.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalRefundAmount = totalRefundAmount.add(lineRefundTotal);

            SalesReturnItem returnItem = SalesReturnItem.builder()
                    .salesReturn(salesReturn)
                    .itemId(itemReq.getItemId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(actualPaidPerUnit)
                    .reason(request.getReason())
                    .build();

            salesReturn.getItems().add(returnItem);

            // --- FIX A: Pass Correct Price for Stock ---
            StockUpdateDto stockDto = StockUpdateDto.builder()
                    .itemId(originalSoldItem.getItemId())
                    .warehouseId(invoice.getWarehouseId())
                    .quantity(itemReq.getQuantity())
                    .transactionType(MovementType.IN)
                    .referenceType(ReferenceType.SALES_RETURN)
                    .referenceId(invoice.getId())
                    .remarks("Returned from Invoice " + invoice.getInvoiceNumber())
                    .batchNumber(batchNum)

                    // CORRECT: Use the Batch Cost so Inventory Value is restored accurately
                    .unitPrice(originalBuyPrice)
                    .build();

            stockService.updateStock(stockDto);
        }

        salesReturn.setTotalAmount(totalRefundAmount);
        SalesReturn savedReturn = salesReturnRepository.save(salesReturn);

        paymentService.createCreditNote(
                invoice.getCustomer(),
                totalRefundAmount,
                savedReturn.getReturnNumber());

        return CommonResponse.builder()
                .id(savedReturn.getId().toString())
                .message("Sales return processed successfully")
                .status(Status.SUCCESS)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SalesReturnDto> getSalesReturns(Integer page, Integer size) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<SalesReturn> data = salesReturnRepository.findByTenantId(tenantId, pageable);

        return data.map(this::mapToDto);
    }


    @Override
    @Transactional(readOnly = true)
    public SalesReturnDto getSalesReturnById(Long id) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        SalesReturn salesReturn = salesReturnRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Sales Return not found with ID: " + id, HttpStatus.NOT_FOUND));

        return mapToDto(salesReturn);
    }



    private SalesReturnDto mapToDto(SalesReturn entity) {

        if (entity == null) return null;

        List<SalesReturnItemDto> items = entity.getItems()
                .stream()
                .map(item -> SalesReturnItemDto.builder()
                        .id(item.getId())
                        .itemId(item.getItemId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .reason(item.getReason())
                        .build()
                ).toList();

        return SalesReturnDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .returnNumber(entity.getReturnNumber())
                .invoiceId(entity.getInvoice().getId())
                .returnDate(entity.getReturnDate())
                .totalAmount(entity.getTotalAmount())
                .items(items)
                .creditNotePaymentId(
                        entity.getCreditNotePayment() != null ?
                                entity.getCreditNotePayment().getId() : null
                )
                .build();
    }

}
