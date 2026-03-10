package com.ezh.Inventory.sales.invoice.service;

import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.sales.delivery.service.DeliveryService;
import com.ezh.Inventory.sales.invoice.dto.InvoiceDto;
import com.ezh.Inventory.sales.invoice.dto.InvoiceFilter;
import com.ezh.Inventory.sales.invoice.dto.InvoiceItemDto;
import com.ezh.Inventory.sales.invoice.entity.*;
import com.ezh.Inventory.sales.invoice.repository.InvoiceRepository;
import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.sales.order.entity.SalesOrderItem;
import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.sales.order.repository.SalesOrderItemRepository;
import com.ezh.Inventory.sales.order.repository.SalesOrderRepository;
import com.ezh.Inventory.stock.dto.StockUpdateDto;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import com.ezh.Inventory.stock.service.StockService;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ItemRepository itemRepository;
    private final StockService stockService;
    private final DeliveryService deliveryService;
    private final AuthServiceClient authServiceClient;


    @Override
    @Transactional
    public CommonResponse<?> createInvoice(InvoiceDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // Get or Auto-Create Sales Order (Restored Logic)
        SalesOrder salesOrder = getOrCreateSalesOrder(dto, tenantId);

        // 2. Initialize Header
        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setWarehouseId(salesOrder.getWarehouseId());
        invoice.setInvoiceNumber(DocumentNumberUtil.generate(DocPrefix.INV));
        invoice.setInvoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : new Date());
        invoice.setSalesOrder(salesOrder);
        invoice.setCustomerId(salesOrder.getCustomerId());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setPaymentStatus(InvoicePaymentStatus.UNPAID);
        invoice.setDeliveryStatus(InvoiceDeliveryStatus.PENDING);
        invoice.setInvoiceType(InvoiceType.CREDIT);
        invoice.setRemarks(dto.getRemarks());
        invoice.setItems(new ArrayList<>());

        // 3. Process Items & Calculate Financials
        processAndCalculateFinancials(invoice, dto);

        // 4. Save to generate ID
        invoice = invoiceRepository.save(invoice);

        // 5. Deduct Stock (Using unified helper) and persist actual batch consumed
        for (InvoiceItem item : invoice.getItems()) {
            String consumedBatch = updateStockHelper(
                    invoice.getWarehouseId(),
                    invoice.getId(),
                    item.getItemId(),
                    item.getQuantity(),
                    MovementType.OUT,
                    item.getBatchNumber()
            );
            item.setBatchNumber(consumedBatch);
        }
        invoiceRepository.save(invoice);

        // create delivery for invoice
        deliveryService.createDeliveryForInvoice(invoice, dto);

        // 6. Update Sales Order Status (Partially/Fully Invoiced)
        updateSalesOrderStatus(salesOrder);

        return CommonResponse.builder()
                .id(invoice.getId().toString())
                .message("Invoice Created Successfully")
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> updateInvoice(Long id, InvoiceDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BadRequestException("Cannot edit an invoice that is already " + invoice.getStatus());
        }

        invoice.setRemarks(dto.getRemarks());
        if (dto.getInvoiceDate() != null) {
            invoice.setInvoiceDate(dto.getInvoiceDate());
        }

        // --- FIX: Revert old stock AND Revert old SO Quantities ---
        for (InvoiceItem oldItem : invoice.getItems()) {
            // 1. Revert Stock
            updateStockHelper(invoice.getWarehouseId(), invoice.getId(), oldItem.getItemId(), oldItem.getQuantity(), MovementType.IN, oldItem.getBatchNumber());

            // 2. Revert Sales Order Invoiced Qty (Fetch from memory to keep sync)
            if (invoice.getSalesOrder() != null && oldItem.getSoItemId() != null) {
                SalesOrderItem soItemToRevert = invoice.getSalesOrder().getItems().stream()
                        .filter(i -> i.getId().equals(oldItem.getSoItemId()))
                        .findFirst().orElse(null);

                if (soItemToRevert != null) {
                    int revertedQty = Math.max(0, soItemToRevert.getInvoicedQty() - oldItem.getQuantity());
                    soItemToRevert.setInvoicedQty(revertedQty);
                }
            }
        }

        invoice.getItems().clear();

        processAndCalculateFinancials(invoice, dto);

        invoiceRepository.save(invoice);

        for (InvoiceItem newItem : invoice.getItems()) {
            String consumedBatch = updateStockHelper(
                    invoice.getWarehouseId(),
                    invoice.getId(),
                    newItem.getItemId(),
                    newItem.getQuantity(),
                    MovementType.OUT,
                    newItem.getBatchNumber()
            );
            newItem.setBatchNumber(consumedBatch);
        }
        invoiceRepository.save(invoice);

        if (invoice.getSalesOrder() != null) {
            updateSalesOrderStatus(invoice.getSalesOrder());
        }

        return CommonResponse.builder()
                .id(invoice.getId().toString())
                .message("Invoice Updated Successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long id) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        Map<Long, UserMiniDto> customerMap = authServiceClient.getBulkUserDetails(List.of(invoice.getCustomerId()));
        return mapToDto(invoice, customerMap, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceDto> getAllInvoices(InvoiceFilter filter, Integer page, Integer size) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size);

        Page<Invoice> invoices = invoiceRepository.getAllInvoices(
                tenantId,
                filter.getId(),
                filter.getSalesOrderId(),
                filter.getInvStatuses(),
                filter.getPaymentStatus(),
                filter.getCustomerId(),
                filter.getWarehouseId(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        Map<Long, UserMiniDto> customerMap = new HashMap<>();
        List<Long> customerIds = invoices.getContent().stream()
                .map(Invoice::getCustomerId)
                .distinct()
                .toList();
        customerMap = authServiceClient.getBulkUserDetails(customerIds);
        final Map<Long, UserMiniDto> finalMap = customerMap;

        return invoices.map(inv -> mapToDto(inv, finalMap, true));
    }


    @Override
    @Transactional
    public CommonResponse<?> updateInvoiceStatus(Long invoiceId, InvoiceStatus status) throws CommonException {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        invoice.setStatus(status);
        invoiceRepository.save(invoice);

        return CommonResponse.builder()
                .message("Invoice status updated successfully")
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDto> searchInvoices(InvoiceFilter filter) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<Invoice> invoices = invoiceRepository.searchInvoices(
                tenantId,
                filter.getId(),
                filter.getInvoiceNumber(),
                filter.getSalesOrderId(),
                filter.getInvStatuses(),
                filter.getCustomerId(),
                filter.getWarehouseId()
        );
        final Map<Long, UserMiniDto> finalMap = new HashMap<>();
        return invoices.stream().map(inv -> mapToDto(inv, finalMap, false)).toList();
    }

    private void updateSalesOrderStatus(SalesOrder salesOrder) {
        // Use the live in-memory list, DO NOT query the repository! ---
        List<SalesOrderItem> allItems = salesOrder.getItems();

        if (allItems == null || allItems.isEmpty()) return;

        boolean allFullyInvoiced = true;
        boolean anyInvoiced = false;

        for (SalesOrderItem item : allItems) {
            int invoiced = item.getInvoicedQty() != null ? item.getInvoicedQty() : 0;
            int ordered = item.getOrderedQty() != null ? item.getOrderedQty() : 0;

            if (invoiced > 0) {
                anyInvoiced = true;
            }
            if (invoiced < ordered) {
                allFullyInvoiced = false;
            }
        }

        // Added fallback to PENDING in case items were removed ---
        if (allFullyInvoiced) {
            salesOrder.setStatus(SalesOrderStatus.FULLY_INVOICED);
        } else if (anyInvoiced) {
            salesOrder.setStatus(SalesOrderStatus.PARTIALLY_INVOICED);
        } else {
            salesOrder.setStatus(SalesOrderStatus.PENDING);
        }

        // Only one save is needed to flush all changes to the database
        salesOrderRepository.save(salesOrder);
    }

    private String updateStockHelper(Long warehouseId, Long invoiceId, Long itemId, int qty, MovementType type, String batch) {
        StockUpdateDto stockUpdate = StockUpdateDto.builder()
                .itemId(itemId)
                .warehouseId(warehouseId)
                .quantity(qty)
                .transactionType(type)
                .referenceType(ReferenceType.SALE)
                .referenceId(invoiceId)
                .batchNumber(batch)
                .build();
        CommonResponse<?> response = stockService.updateStock(stockUpdate);
        Object data = response != null ? response.getData() : null;
        return data != null ? data.toString() : batch;
    }

    private InvoiceDto mapToDto(Invoice invoice, Map<Long, UserMiniDto> customerMap, boolean includeContact) {

        // 1. Map Child Items
        List<InvoiceItemDto> itemDtos = new ArrayList<>();
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                itemDtos.add(InvoiceItemDto.builder()
                        .id(item.getId())
                        .soItemId(item.getSoItemId())
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .sku(item.getSku())
                        .batchNumber(item.getBatchNumber())
                        .returnedQuantity(item.getReturnedQuantity())
                        // Financial Line Fields
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountRate(item.getDiscountRate())
                        .discountAmount(item.getDiscountAmount())
                        .taxRate(item.getTaxRate())
                        .taxAmount(item.getTaxAmount())
                        .lineTotal(item.getLineTotal())
                        .build());
            }
        }

        // 2. Handle Customer Contact Info (If requested)
        UserMiniDto contactMini = null;
        if (includeContact && customerMap != null) {
            UserMiniDto userDetail = customerMap.getOrDefault(invoice.getCustomerId(), new UserMiniDto());
            contactMini = UserMiniDto.builder()
                    .id(userDetail.getId())
                    .userType(userDetail.getUserType())
                    .userUuid(userDetail.getUserUuid())
                    .name(userDetail.getName())
                    .email(userDetail.getEmail())
                    .phone(userDetail.getPhone())
                    .build();
        }

        // 3. Map Header & Return
        return InvoiceDto.builder()
                // Identity
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())

                // Relationships
                .salesOrderId(invoice.getSalesOrder() != null ? invoice.getSalesOrder().getId() : null)
                .salesOrderNumber(invoice.getSalesOrder() != null ? invoice.getSalesOrder().getOrderNumber() : null)
                .warehouseId(invoice.getWarehouseId())
                .customerId(invoice.getCustomerId())
                .contactMini(contactMini)

                // Status & Info
                .invoiceDate(invoice.getInvoiceDate())
                .status(invoice.getStatus())
                .deliveryStatus(invoice.getDeliveryStatus())
                .paymentStatus(invoice.getPaymentStatus())
                .invoiceType(invoice.getInvoiceType())
                .remarks(invoice.getRemarks())

                // Financial Header Fields
                .itemGrossTotal(invoice.getItemGrossTotal())
                .itemTotalDiscount(invoice.getItemTotalDiscount())
                .itemTotalTax(invoice.getItemTotalTax())

                .flatDiscountRate(invoice.getFlatDiscountRate())
                .flatDiscountAmount(invoice.getFlatDiscountAmount())

                .flatTaxRate(invoice.getFlatTaxRate())
                .flatTaxAmount(invoice.getFlatTaxAmount())

                .grandTotal(invoice.getGrandTotal())
                .amountPaid(invoice.getAmountPaid())
                .balance(invoice.getBalance())

                // Attach Items
                .items(itemDtos)
                .build();
    }

    /**
     * THE UNIFIED FINANCIAL PIPELINE
     * 1. Maps DTO items to Entities
     * 2. Calculates Line Level Math (Rate -> Amount)
     * 3. Aggregates Item Totals
     * 4. Calculates Header Level Math (Flat Adjustments)
     */
    private void processAndCalculateFinancials(Invoice invoice, InvoiceDto dto) {
        BigDecimal itemGrossTotal = BigDecimal.ZERO;
        BigDecimal itemTotalDiscount = BigDecimal.ZERO;
        BigDecimal itemTotalTax = BigDecimal.ZERO;

        if (dto.getItems() != null) {
            for (InvoiceItemDto itemDto : dto.getItems()) {

                // A. Validate & Fetch Master Data
                SalesOrderItem soItem = invoice.getSalesOrder().getItems().stream()
                        .filter(item -> item.getId().equals(itemDto.getSoItemId()))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Invalid SO Line Item ID"));

                Item itemMaster = itemRepository.findById(itemDto.getItemId())
                        .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));


                // B. Line Calculations
                BigDecimal qty = BigDecimal.valueOf(itemDto.getQuantity());
                BigDecimal price = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : soItem.getUnitPrice();

                // Gross
                BigDecimal grossPrice = price.multiply(qty);

                // Discount (Rate -> Amount)
                BigDecimal discRate = itemDto.getDiscountRate() != null ? itemDto.getDiscountRate() : BigDecimal.ZERO;
                BigDecimal discAmt = grossPrice.multiply(discRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Tax (Rate -> Amount on Taxable Value)
                BigDecimal taxableValue = grossPrice.subtract(discAmt);
                BigDecimal taxRate = itemDto.getTaxRate() != null ? itemDto.getTaxRate() : BigDecimal.ZERO;
                BigDecimal taxAmt = taxableValue.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Line Total
                BigDecimal lineTotal = taxableValue.add(taxAmt);

                // C. Build Entity
                InvoiceItem invItem = new InvoiceItem();
                invItem.setInvoice(invoice);
                invItem.setSoItemId(soItem.getId());
                invItem.setItemId(itemMaster.getId());
                invItem.setItemName(itemMaster.getName());
                invItem.setSku(itemMaster.getSku());
                invItem.setBatchNumber(itemDto.getBatchNumber()); // User can override batch

                // Financials (AbstractFinancialLine)
                invItem.setQuantity(itemDto.getQuantity());
                invItem.setUnitPrice(price);
                invItem.setDiscountRate(discRate);
                invItem.setDiscountAmount(discAmt);
                invItem.setTaxRate(taxRate);
                invItem.setTaxAmount(taxAmt);
                invItem.setLineTotal(lineTotal);

                invoice.getItems().add(invItem);
                soItem.setInvoicedQty(soItem.getInvoicedQty() + itemDto.getQuantity());

                // Accumulate
                itemGrossTotal = itemGrossTotal.add(grossPrice);
                itemTotalDiscount = itemTotalDiscount.add(discAmt);
                itemTotalTax = itemTotalTax.add(taxAmt);
            }
        }

        // Set Aggregates
        invoice.setItemGrossTotal(itemGrossTotal);
        invoice.setItemTotalDiscount(itemTotalDiscount);
        invoice.setItemTotalTax(itemTotalTax);

        // --- Header Level Flat Adjustments ---

        // Base for flat calc is usually the sum of line totals (Net Bill)
        BigDecimal lineSum = invoice.getItems().stream()
                .map(InvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Flat Discount
        BigDecimal flatDiscRate = dto.getFlatDiscountRate() != null ? dto.getFlatDiscountRate() : BigDecimal.ZERO;
        BigDecimal flatDiscAmt = lineSum.multiply(flatDiscRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setFlatDiscountRate(flatDiscRate);
        invoice.setFlatDiscountAmount(flatDiscAmt);

        // Flat Tax (on discounted bill)
        BigDecimal discountedBill = lineSum.subtract(flatDiscAmt);
        BigDecimal flatTaxRate = dto.getFlatTaxRate() != null ? dto.getFlatTaxRate() : BigDecimal.ZERO;
        BigDecimal flatTaxAmt = discountedBill.multiply(flatTaxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setFlatTaxRate(flatTaxRate);
        invoice.setFlatTaxAmount(flatTaxAmt);

        // Grand Total
        BigDecimal grandTotal = discountedBill.add(flatTaxAmt);
        invoice.setGrandTotal(grandTotal.max(BigDecimal.ZERO));

        // Initial Balance is full amount
        if (invoice.getId() == null) {
            invoice.setBalance(invoice.getGrandTotal());
            invoice.setAmountPaid(BigDecimal.ZERO);
        }
    }


    /**
     * RESTORED: Get or Auto-Create Sales Order Logic
     * Updated to support the new Rate/Amount structure.
     */
    private SalesOrder getOrCreateSalesOrder(InvoiceDto dto, Long tenantId) {
        // Scenario 1: Invoice generated from existing SO
        if (dto.getSalesOrderId() != null) {
            SalesOrder so = salesOrderRepository.findByIdAndTenantId(dto.getSalesOrderId(), tenantId)
                    .orElseThrow(() -> new CommonException("Sales order not found", HttpStatus.NOT_FOUND));

            if (so.getStatus() == SalesOrderStatus.FULLY_INVOICED || so.getStatus() == SalesOrderStatus.CANCELLED) {
                throw new BadRequestException("Sales Order is already completed or cancelled");
            }
            return so;
        }

        // Scenario 2: Direct Invoice (Creates an SO behind the scenes)
        if (dto.getWarehouseId() == null) {
            throw new BadRequestException("Warehouse ID is required for direct invoices");
        }

        // A. Create Header (Using Setters to match inheritance)
        SalesOrder newSo = new SalesOrder();
        newSo.setTenantId(tenantId);
        newSo.setWarehouseId(dto.getWarehouseId());
        newSo.setCustomerId(dto.getCustomerId());
        newSo.setOrderNumber(DocumentNumberUtil.generate(DocPrefix.SO));
        newSo.setOrderDate(new Date());
        newSo.setStatus(SalesOrderStatus.CREATED);
        newSo.setSource(SalesOrderSource.DIRECT_SALES);
        newSo.setRemarks("Auto-generated from Direct Invoice");
        newSo.setItems(new ArrayList<>());

        BigDecimal grossTotal = BigDecimal.ZERO;

        for (InvoiceItemDto itemDto : dto.getItems()) {
            Item itemMaster = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new CommonException("Item not found", HttpStatus.NOT_FOUND));

            BigDecimal price = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : itemMaster.getSellingPrice();
            BigDecimal lineGross = price.multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            // Build Item via Setters
            SalesOrderItem soItem = new SalesOrderItem();
            soItem.setSalesOrder(newSo);
            soItem.setItemId(itemMaster.getId());
            soItem.setItemName(itemMaster.getName());
            soItem.setOrderedQty(itemDto.getQuantity());
            soItem.setQuantity(itemDto.getQuantity());
            soItem.setInvoicedQty(0);

            // Financials
            soItem.setQuantity(itemDto.getQuantity());
            soItem.setUnitPrice(price);
            soItem.setDiscountRate(itemDto.getDiscountRate() != null ? itemDto.getDiscountRate() : BigDecimal.ZERO);
            soItem.setTaxRate(itemDto.getTaxRate() != null ? itemDto.getTaxRate() : BigDecimal.ZERO);

            // Simplified line total for direct SO creation (Real math runs in Invoice)
            soItem.setLineTotal(lineGross);

            newSo.getItems().add(soItem);
            grossTotal = grossTotal.add(lineGross);
        }

        newSo.setItemGrossTotal(grossTotal);
        newSo.setGrandTotal(grossTotal);

        // Save to generate IDs
        SalesOrder savedSo = salesOrderRepository.save(newSo);

        // B. CRITICAL: Inject new SO Item IDs back into the DTO
        // This ensures the Invoice logic can find the parent SO line.
        for (int i = 0; i < savedSo.getItems().size(); i++) {
            dto.getItems().get(i).setSoItemId(savedSo.getItems().get(i).getId());
        }

        return savedSo;
    }
}
