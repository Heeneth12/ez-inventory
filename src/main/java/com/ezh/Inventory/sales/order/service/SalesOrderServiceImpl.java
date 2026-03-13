package com.ezh.Inventory.sales.order.service;

import com.ezh.Inventory.approval.dto.ApprovalCheckContext;
import com.ezh.Inventory.approval.entity.ApprovalResultStatus;
import com.ezh.Inventory.approval.entity.ApprovalStatus;
import com.ezh.Inventory.approval.entity.ApprovalType;
import com.ezh.Inventory.approval.service.ApprovalService;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.sales.order.dto.*;
import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.sales.order.entity.SalesOrderItem;
import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.sales.order.repository.SalesOrderRepository;
import com.ezh.Inventory.sales.order.utils.SalesOrderExportUtils;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonFilter;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.common.events.ApprovalDecisionEvent;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ItemRepository itemRepository;
    private final ApprovalService approvalService;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public CommonResponse<?> createSalesOrder(SalesOrderDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        //Initialize Header
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setTenantId(tenantId);
        salesOrder.setWarehouseId(dto.getWarehouseId());
        salesOrder.setCustomerId(dto.getCustomerId());
        salesOrder.setOrderNumber(DocumentNumberUtil.generate(DocPrefix.SO));
        salesOrder.setOrderDate(new Date());
        salesOrder.setStatus(SalesOrderStatus.CREATED);
        salesOrder.setSource(SalesOrderSource.SALES_TEAM);
        salesOrder.setRemarks(dto.getRemarks());
        salesOrder.setItems(new ArrayList<>());

        //Process Items & Calculate Financials (The Unified Pipeline)
        processAndCalculateFinancials(salesOrder, dto);

        //Save Order (to generate ID for approval context)
        salesOrder = salesOrderRepository.save(salesOrder);

        //Calculate Total Discount Percentage for Approval Rule
        // Formula: ((Total Item Discounts + Flat Discount) / Item Gross Total) * 100
        double totalDiscountPercentage = 0.0;
        BigDecimal totalCombinedDiscount = salesOrder.getItemTotalDiscount().add(salesOrder.getFlatDiscountAmount());

        if (salesOrder.getItemGrossTotal().compareTo(BigDecimal.ZERO) > 0) {
            totalDiscountPercentage = totalCombinedDiscount
                    .divide(salesOrder.getItemGrossTotal(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        //Build Approval Context
        ApprovalCheckContext approvalCheckContext = ApprovalCheckContext.builder()
                .type(ApprovalType.SALES_ORDER_DISCOUNT)
                .amount(salesOrder.getGrandTotal())
                .percentage(totalDiscountPercentage)
                .referenceId(salesOrder.getId())
                .referenceCode(salesOrder.getOrderNumber())
                .build();

        //Check Approval & Update Status
        CommonResponse<?> approvalResponse = approvalService.checkAndInitiateApproval(approvalCheckContext);
        if (approvalResponse.getData() == ApprovalResultStatus.APPROVAL_REQUIRED) {
            salesOrder.setStatus(SalesOrderStatus.PENDING_APPROVAL);
        } else {
            salesOrder.setStatus(SalesOrderStatus.CONFIRMED);
        }

        salesOrderRepository.save(salesOrder);

        return CommonResponse.builder()
                .id(salesOrder.getId().toString())
                .message("Sales Order Created Successfully")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> updateSalesOrder(Long id, SalesOrderDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        SalesOrder salesOrder = salesOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Sales Order not found", HttpStatus.NOT_FOUND));

        List<SalesOrderStatus> editableStatuses = List.of(
                SalesOrderStatus.CREATED,
                SalesOrderStatus.PENDING,
                SalesOrderStatus.CONFIRMED,
                SalesOrderStatus.PENDING_APPROVAL
        );

        if (!editableStatuses.contains(salesOrder.getStatus())) {
            throw new BadRequestException("Cannot edit Sales Order when status is: " + salesOrder.getStatus());
        }

        salesOrder.setWarehouseId(dto.getWarehouseId());
        salesOrder.setCustomerId(dto.getCustomerId());
        salesOrder.setRemarks(dto.getRemarks());
        if (dto.getOrderDate() != null) salesOrder.setOrderDate(dto.getOrderDate());

        //Re-calculate everything cleanly
        processAndCalculateFinancials(salesOrder, dto);

        salesOrderRepository.save(salesOrder);

        return CommonResponse.builder()
                .id(salesOrder.getId().toString())
                .message("Sales Order Updated")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(Long id) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        SalesOrder so = salesOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Order not found", HttpStatus.BAD_REQUEST));

        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(so.getCustomerId()));

        return mapToDto(so, customerMap, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter, int page, int size) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Pageable pageable = PageRequest.of(page, size);

        Page<SalesOrder> result = salesOrderRepository.getAllSalesOrders(
                tenantId, filter.getId(), filter.getSoStatuses(), filter.getSoSource(), filter.getCustomerId(),
                filter.getWarehouseId(), filter.getSearchQuery(), filter.getStartDateTime(),
                filter.getEndDateTime(), pageable);

        // 1. Collect all unique Customer IDs from the page
        Set<Long> customerIds = result.getContent().stream()
                .map(SalesOrder::getCustomerId)
                .collect(Collectors.toSet());

        // 2. Fetch details from external service in ONE call
        Map<Long, UserMiniDto> finalCustomerDetails = new HashMap<>();
        if (!customerIds.isEmpty()) {
            finalCustomerDetails = authServiceClient.getBulkUserDetails(new ArrayList<>(customerIds));
        }

        // 3. Map to DTO using the fetched details
        final Map<Long, UserMiniDto> customerMapLookup = finalCustomerDetails;
        return result.map(so -> mapToDto(so, customerMapLookup, true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<SalesOrder> orders = salesOrderRepository.getAllSalesOrders(
                tenantId,
                filter.getId(),
                filter.getSoNumber(),
                filter.getSoStatuses(),
                filter.getCustomerId(),
                filter.getWarehouseId()
        );

        return orders.stream()
                .map(so -> mapToDto(so, null, false))
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public SalesOrderStats getStats(CommonFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        return salesOrderRepository.getDashboardStats(
                tenantId,
                filter.getStartDateTime(),
                filter.getEndDateTime()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public byte[] downloadSalesOrdersExcel(SalesOrderFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<SalesOrder> orders = salesOrderRepository.getAllSalesOrders(
                tenantId,
                filter.getId(),
                filter.getSoStatuses(),
                filter.getSoSource(),
                filter.getCustomerId(),
                filter.getWarehouseId(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime()
        );

        List<SalesOrderExcelRowDto> rows = orders.stream()
                .map(so -> SalesOrderExcelRowDto.builder()
                        .id(so.getId())
                        .orderNumber(so.getOrderNumber())
                        .orderDate(so.getOrderDate())
                        .status(so.getStatus() != null ? so.getStatus().name() : null)
                        .source(so.getSource() != null ? so.getSource().name() : null)
                        .customerId(so.getCustomerId())
                        .warehouseId(so.getWarehouseId())
                        .itemGrossTotal(so.getItemGrossTotal())
                        .itemTotalDiscount(so.getItemTotalDiscount())
                        .itemTotalTax(so.getItemTotalTax())
                        .grandTotal(so.getGrandTotal())
                        .remarks(so.getRemarks())
                        .build())
                .toList();

        return SalesOrderExportUtils.toExcel(rows).readAllBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesConversionReportDto getSalesOrderConversionReport(CommonFilter filter) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        SalesConversionCountProjection projection = salesOrderRepository.countSalesOrderConversion(
                tenantId,
                null,
                filter.getWarehouseId(),
                filter.getStartDateTime(),
                filter.getEndDateTime()
        );

        long total = projection != null && projection.getTotalSalesOrders() != null ? projection.getTotalSalesOrders() : 0L;
        long converted = projection != null && projection.getConvertedToInvoice() != null ? projection.getConvertedToInvoice() : 0L;
        long pending = Math.max(total - converted, 0L);
        BigDecimal conversionRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(converted)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return SalesConversionReportDto.builder()
                .totalSalesOrders(total)
                .convertedToInvoice(converted)
                .pendingConversion(pending)
                .conversionRate(conversionRate)
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> updateStatus(Long id, SalesOrderStatus newStatus) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        SalesOrder so = salesOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Order not found", HttpStatus.BAD_REQUEST));

        SalesOrderStatus currentStatus = so.getStatus();

        if (currentStatus == newStatus) {
            return CommonResponse.builder()
                    .message("Order is already in " + newStatus.name() + " status")
                    .build();
        }

        if (newStatus == SalesOrderStatus.CANCELLED) {
            // Rule A: Cannot cancel if status is already invoiced
            if (currentStatus == SalesOrderStatus.FULLY_INVOICED ||
                    currentStatus == SalesOrderStatus.PARTIALLY_INVOICED) {
                throw new BadRequestException("Cannot cancel an order that is partially or fully invoiced.");
            }

            // Rule B: Extra safety - Check if any individual items have been invoiced behind the scenes
            boolean hasInvoicedItems = so.getItems().stream().anyMatch(i -> i.getInvoicedQty() > 0);
            if (hasInvoicedItems) {
                throw new BadRequestException("Cannot cancel order with invoiced items. Please return items first.");
            }
        }
        //Apply the new status and save
        so.setStatus(newStatus);
        salesOrderRepository.save(so);

        return CommonResponse.builder()
                .message("Order status successfully updated to " + newStatus.name())
                .build();
    }


    @EventListener
    @Transactional
    public void onApprovalDecision(ApprovalDecisionEvent event) throws CommonException {

        if (event.getType() != ApprovalType.SALES_ORDER_DISCOUNT) {
            return;
        }

        SalesOrder so = salesOrderRepository.findById(event.getReferenceId())
                .orElseThrow(() -> new CommonException("Linked Sales Order not found", HttpStatus.NOT_FOUND));

        if (event.getStatus() == ApprovalStatus.APPROVED) {
            so.setStatus(SalesOrderStatus.PENDING);
        } else {
            so.setStatus(SalesOrderStatus.REJECTED);
        }

        salesOrderRepository.save(so);
    }

    /**
     * THE UNIFIED FINANCIAL PIPELINE
     * Calculates Item Level Math -> Aggregates -> Calculates Header Level Math
     */
    private void processAndCalculateFinancials(SalesOrder salesOrder, SalesOrderDto dto) {
        BigDecimal itemGrossTotal = BigDecimal.ZERO;
        BigDecimal itemTotalDiscount = BigDecimal.ZERO;
        BigDecimal itemTotalTax = BigDecimal.ZERO;

        if (salesOrder.getItems() == null) {
            salesOrder.setItems(new ArrayList<>());
        } else {
            salesOrder.getItems().clear(); // Clear for updates
        }

        // --- 1. Process Line Items ---
        if (dto.getItems() != null) {
            for (SalesOrderItemDto itemDto : dto.getItems()) {
                Item itemMaster = itemRepository.findById(itemDto.getItemId())
                        .orElseThrow(() -> new CommonException("Item ID not found", HttpStatus.BAD_REQUEST));

                BigDecimal qty = BigDecimal.valueOf(itemDto.getOrderedQty());
                BigDecimal price = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : itemMaster.getSellingPrice();

                // Gross
                BigDecimal grossPrice = price.multiply(qty);

                // Item Discount
                BigDecimal discRate = itemDto.getDiscountRate() != null ? itemDto.getDiscountRate() : BigDecimal.ZERO;
                BigDecimal discAmt = grossPrice.multiply(discRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Item Tax (Applied to Taxable Value)
                BigDecimal taxableValue = grossPrice.subtract(discAmt);
                BigDecimal taxRate = itemDto.getTaxRate() != null ? itemDto.getTaxRate() : BigDecimal.ZERO;
                BigDecimal taxAmt = taxableValue.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Line Total
                BigDecimal lineTotal = taxableValue.add(taxAmt);

                // Create Item Entity
                SalesOrderItem soItem = new SalesOrderItem();
                soItem.setSalesOrder(salesOrder);
                soItem.setItemId(itemMaster.getId());
                soItem.setItemName(itemMaster.getName());
                soItem.setQuantity(itemDto.getOrderedQty());
                soItem.setOrderedQty(itemDto.getOrderedQty());
                soItem.setUnitPrice(price);
                soItem.setDiscountRate(discRate);
                soItem.setDiscountAmount(discAmt);
                soItem.setTaxRate(taxRate);
                soItem.setTaxAmount(taxAmt);
                soItem.setLineTotal(lineTotal);

                salesOrder.getItems().add(soItem);

                // Accumulate totals
                itemGrossTotal = itemGrossTotal.add(grossPrice);
                itemTotalDiscount = itemTotalDiscount.add(discAmt);
                itemTotalTax = itemTotalTax.add(taxAmt);
            }
        }

        salesOrder.setItemGrossTotal(itemGrossTotal);
        salesOrder.setItemTotalDiscount(itemTotalDiscount);
        salesOrder.setItemTotalTax(itemTotalTax);

        //Process Header (Flat) Adjustments

        // Base line sum (Total of all item lineTotals)
        BigDecimal lineSum = salesOrder.getItems().stream()
                .map(SalesOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Flat Discount
        BigDecimal flatDiscRate = dto.getFlatDiscountRate() != null ? dto.getFlatDiscountRate() : BigDecimal.ZERO;
        BigDecimal flatDiscAmt = lineSum.multiply(flatDiscRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        salesOrder.setFlatDiscountRate(flatDiscRate);
        salesOrder.setFlatDiscountAmount(flatDiscAmt);

        // Flat Tax (Applied on the discounted bill)
        BigDecimal discountedBill = lineSum.subtract(flatDiscAmt);
        BigDecimal flatTaxRate = dto.getFlatTaxRate() != null ? dto.getFlatTaxRate() : BigDecimal.ZERO;
        BigDecimal flatTaxAmt = discountedBill.multiply(flatTaxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        salesOrder.setFlatTaxRate(flatTaxRate);
        salesOrder.setFlatTaxAmount(flatTaxAmt);

        // Grand Total
        salesOrder.setGrandTotal(discountedBill.add(flatTaxAmt));
    }

    private SalesOrderDto mapToDto(SalesOrder so, Map<Long, UserMiniDto> customerMap, boolean includeContact) {
        List<SalesOrderItemDto> itemDtos = new ArrayList<>();

        if (so.getItems() != null) {
            for (SalesOrderItem item : so.getItems()) {
                itemDtos.add(SalesOrderItemDto.builder()
                        .id(item.getId())
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .orderedQty(item.getOrderedQty())
                        .invoicedQty(item.getInvoicedQty())
                        .unitPrice(item.getUnitPrice())
                        .discountRate(item.getDiscountRate())
                        .discountAmount(item.getDiscountAmount())
                        .taxRate(item.getTaxRate())
                        .taxAmount(item.getTaxAmount())
                        .lineTotal(item.getLineTotal())
                        .build()
                );
            }
        }

        UserMiniDto contactMini = null;
        String customerName = null;
        if (includeContact && customerMap != null) {
            contactMini = customerMap.getOrDefault(so.getCustomerId(), new UserMiniDto());
            customerName = contactMini.getName();
        }

        return SalesOrderDto.builder()
                .id(so.getId())
                .orderNumber(so.getOrderNumber())
                .orderDate(so.getOrderDate())
                .status(so.getStatus())
                .source(so.getSource())
                .warehouseId(so.getWarehouseId())
                .customerId(so.getCustomerId())
                .contactMini(contactMini)
                .customerName(customerName)
                .remarks(so.getRemarks())
                .itemGrossTotal(so.getItemGrossTotal())
                .itemTotalDiscount(so.getItemTotalDiscount())
                .itemTotalTax(so.getItemTotalTax())
                .flatDiscountRate(so.getFlatDiscountRate())
                .flatDiscountAmount(so.getFlatDiscountAmount())
                .flatTaxRate(so.getFlatTaxRate())
                .flatTaxAmount(so.getFlatTaxAmount())
                // Aggregated Totals for UI (Item Totals + Flat Totals)
                .totalTax(so.getItemTotalTax().add(so.getFlatTaxAmount()))
                .totalDiscount(so.getItemTotalDiscount().add(so.getFlatDiscountAmount()))
                .grandTotal(so.getGrandTotal())
                .items(itemDtos)
                .build();
    }
}