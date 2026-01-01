package com.ezh.Inventory.sales.invoice.service;

import com.ezh.Inventory.contacts.dto.ContactMiniDto;
import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.sales.delivery.service.DeliveryService;
import com.ezh.Inventory.sales.invoice.dto.*;
import com.ezh.Inventory.sales.invoice.entity.*;
import com.ezh.Inventory.sales.invoice.repository.InvoiceItemRepository;
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
import com.ezh.Inventory.utils.common.Status;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ItemRepository itemRepository;
    private final ContactRepository contactRepository;
    private final StockService stockService;
    private final DeliveryService deliveryService;


    @Override
    @Transactional
    public CommonResponse createInvoice(InvoiceCreateDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        SalesOrder salesOrder = getOrCreateSalesOrder(dto, tenantId);

        Contact contact = contactRepository.findByIdAndTenantId(dto.getCustomerId(), tenantId)
                .orElseThrow(() -> new CommonException("Customer not found", HttpStatus.NOT_FOUND));

        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .warehouseId(salesOrder.getWarehouseId())
                .invoiceNumber(DocumentNumberUtil.generate(DocPrefix.INV))
                .invoiceDate(new Date())
                .salesOrder(salesOrder)
                .customer(contact)
                .status(InvoiceStatus.PENDING)
                .paymentStatus(InvoicePaymentStatus.UNPAID)
                .deliveryStatus(InvoiceDeliveryStatus.PENDING)
                .items(new ArrayList<>())
                .remarks(dto.getRemarks())
                .invoiceType(InvoiceType.CREDIT)
                //Now this SAVE will work because all non-null fields have values
                .subTotal(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build();

        invoice = invoiceRepository.save(invoice);

        processInvoiceItems(invoice, dto.getItems());

        //Finalize Financials (Apply Header Level Flat Logic)
        finalizeInvoiceTotals(invoice, dto);

        invoiceRepository.save(invoice);

        //TRIGGER DELIVERY LOGIC
        deliveryService.createDeliveryForInvoice(invoice, dto);

        // F. Update Sales Order Status
        updateSalesOrderStatus(salesOrder);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(invoice.getId().toString())
                .message("Invoice Created Successfully")
                .build();
    }


    @Override
    @Transactional
    public CommonResponse updateInvoice(Long id, InvoiceCreateDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        //Fetch Existing Invoice
        Invoice invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CommonException("Invoice not found", HttpStatus.NOT_FOUND));

        //Validation: Prevent editing if already Processed/Paid
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BadRequestException("Cannot edit invoice with status: " + invoice.getStatus());
        }

        //Update Header Fields
        invoice.setRemarks(dto.getRemarks());
        invoice.setInvoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : invoice.getInvoiceDate());

        // NOTE: Changing Warehouse or Customer often requires full reset.
        // Assuming Warehouse is locked for now to prevent cross-warehouse stock corruption.

        //Process Item Changes (The Core Logic)
        updateInvoiceItems(invoice, dto.getItems());

        // 5. Recalculate Financials
        finalizeInvoiceTotals(invoice, dto);

        // 6. Save Updates
        invoiceRepository.save(invoice);

        // 7. Sync Sales Order Status
        if (invoice.getSalesOrder() != null) {
            updateSalesOrderStatus(invoice.getSalesOrder());
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
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

        return mapToDto(invoice);
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
                filter.getStatus(),
                filter.getCustomerId(),
                filter.getWarehouseId(),
                filter.getSearchQuery(),
                filter.getFromDate(),
                filter.getToDate(),
                pageable
        );

        return invoices.map(this::mapToDto);
    }


    @Override
    @Transactional
    public CommonResponse updateInvoiceStatus(Long invoiceId, InvoiceStatus status) throws CommonException {

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

        InvoiceStatus status = null;
        status = InvoiceStatus.valueOf(String.valueOf(filter.getStatus()));

        List<Invoice> invoices = invoiceRepository.searchInvoices(
                tenantId,
                filter.getId(),
                filter.getSalesOrderId(),
                status,
                filter.getCustomerId(),
                filter.getWarehouseId()
        );

        return invoices.stream().map(this::mapToDto).toList();
    }

    private void processInvoiceItems(Invoice invoice, List<InvoiceItemCreateDto> itemDtos) {

        if (itemDtos == null || itemDtos.isEmpty()) {
            throw new BadRequestException("Invoice must contain at least one item");
        }

        for (InvoiceItemCreateDto itemDto : itemDtos) {

            // 1. Fetch & Validate SO Line
            SalesOrderItem soItem = salesOrderItemRepository.findById(itemDto.getSoItemId())
                    .orElseThrow(() -> new BadRequestException("Invalid SO Line Item ID"));

            validateItemBelongsToOrder(soItem, invoice.getSalesOrder());
            validateRemainingQuantity(soItem, itemDto.getQuantity());

            // 2. Fetch Master Data
            Item itemMaster = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new CommonException("Item Master not found", HttpStatus.NOT_FOUND));

            // 3. CALL STOCK DEDUCTION (This updates DB and returns the used Batch)
            String allocatedBatchNumber = deductStock(invoice, itemDto);

            // 4. Update DTO so calculateAndMapItem uses the correct batch
            itemDto.setBatchNumber(allocatedBatchNumber);

            // 5. Build Item
            InvoiceItem invoiceItem = calculateAndMapItem(invoice, itemDto, soItem, itemMaster);

            // Double ensure the entity gets the batch
            invoiceItem.setBatchNumber(allocatedBatchNumber);

            invoice.getItems().add(invoiceItem);

            // REMOVED THE SECOND deductStock CALL HERE (It was causing double deduction)

            // 6. Update Progress on Sales Order
            soItem.setInvoicedQty(soItem.getInvoicedQty() + itemDto.getQuantity());
            salesOrderItemRepository.save(soItem);
        }
    }

    //Handle Add, Edit, Delete
    private void updateInvoiceItems(Invoice invoice, List<InvoiceItemCreateDto> incomingDtos) {
        List<InvoiceItem> existingItems = invoice.getItems();
        List<InvoiceItem> itemsToDelete = new ArrayList<>();

        // Map for easy lookup: ID -> DTO
        // We filter out DTOs that are "New" (null ID)
        var incomingMap = incomingDtos.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(InvoiceItemCreateDto::getId, d -> d));

        // A. IDENTIFY MODIFIED & DELETED ITEMS
        for (InvoiceItem existingItem : existingItems) {
            InvoiceItemCreateDto matchedDto = incomingMap.get(existingItem.getId());

            if (matchedDto != null) {
                //UPDATE Existing Item
                handleItemModification(invoice, existingItem, matchedDto);
            } else {
                //DELETE Item (Exists in DB, not in DTO)
                handleItemDeletion(invoice, existingItem);
                itemsToDelete.add(existingItem);
            }
        }

        // Remove deleted items from the parent list
        invoice.getItems().removeAll(itemsToDelete);
        // (Optional) Hard delete from Repo if Cascade doesn't handle it immediately
        invoiceItemRepository.deleteAll(itemsToDelete);

        // B. IDENTIFY NEW ITEMS (DTOs with null ID)
        for (InvoiceItemCreateDto dto : incomingDtos) {
            if (dto.getId() == null) {
                // --- CASE 3: CREATE New Item ---
                handleItemCreation(invoice, dto);
            }
        }
    }

    //Calculate Single Item (Item Level Logic) ---
    private InvoiceItem calculateAndMapItem(Invoice invoice, InvoiceItemCreateDto dto, SalesOrderItem soItem, Item master) {
        //Base Values (Allow override from DTO, fallback to SO Price)
        BigDecimal quantity = BigDecimal.valueOf(dto.getQuantity());
        BigDecimal price = dto.getUnitPrice() != null ? dto.getUnitPrice() : soItem.getUnitPrice();

        //Item Level Adjustments
        BigDecimal itemDiscount = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal itemTax = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;

        //Calculate Line Total
        //Formula: (Price * Qty) - Discount + Tax
        BigDecimal grossTotal = price.multiply(quantity);
        BigDecimal lineTotal = grossTotal.subtract(itemDiscount).add(itemTax);

        return InvoiceItem.builder()
                .invoice(invoice)
                .soItemId(soItem.getId())
                .itemId(master.getId())
                .itemName(master.getName())
                .sku(master.getSku())
                .batchNumber(dto.getBatchNumber())
                .quantity(dto.getQuantity())
                .unitPrice(price)
                .discountAmount(itemDiscount) // Store per-item discount
                .taxAmount(itemTax)           // Store per-item tax
                .lineTotal(lineTotal)
                .build();
    }

    //Final Aggregation (Header Level Logic) ---
    private void finalizeInvoiceTotals(Invoice invoice, InvoiceCreateDto dto) {
        // 1. Initialize Accumulators
        BigDecimal sumGrossAmount = BigDecimal.ZERO; // Pure (Price * Qty)
        BigDecimal sumItemDiscounts = BigDecimal.ZERO;
        BigDecimal sumItemTaxes = BigDecimal.ZERO;

        // 2. Sum up processed items
        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal itemGross = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            sumGrossAmount = sumGrossAmount.add(itemGross);

            sumItemDiscounts = sumItemDiscounts.add(item.getDiscountAmount());
            sumItemTaxes = sumItemTaxes.add(item.getTaxAmount());
        }

        // 3. Get Header Level Flat Inputs
        BigDecimal headerFlatDiscount = dto.getTotalDiscount() != null ? dto.getTotalDiscount() : BigDecimal.ZERO;
        BigDecimal headerFlatTax = dto.getTotalTax() != null ? dto.getTotalTax() : BigDecimal.ZERO;

        // 4. Calculate Final Header Fields
        // Total Discount = (Sum Item Disc) + (Header Flat Disc)
        BigDecimal finalTotalDiscount = sumItemDiscounts.add(headerFlatDiscount);

        // Total Tax = (Sum Item Tax) + (Header Flat Tax)
        BigDecimal finalTotalTax = sumItemTaxes.add(headerFlatTax);

        // 5. Set Values on Entity
        invoice.setSubTotal(sumGrossAmount); // Gross Goods Value
        invoice.setTotalDiscount(finalTotalDiscount);
        invoice.setTotalTax(finalTotalTax);

        // Grand Total = SubTotal - TotalDiscount + TotalTax
        BigDecimal grandTotal = sumGrossAmount.subtract(finalTotalDiscount).add(finalTotalTax);

        invoice.setGrandTotal(grandTotal.max(BigDecimal.ZERO));
        invoice.setBalance(invoice.getGrandTotal()); // Initial balance is the full amount
        invoice.setAmountPaid(BigDecimal.ZERO);
    }

    private void validateRemainingQuantity(SalesOrderItem soItem, Integer invoiceQty) {
        int remaining = soItem.getOrderedQty() - soItem.getInvoicedQty();
        if (invoiceQty > remaining) {
            throw new BadRequestException("Cannot invoice " + invoiceQty + ". Only " + remaining + " remaining for " + soItem.getItemName());
        }
    }

    private void validateItemBelongsToOrder(SalesOrderItem soItem, SalesOrder order) {
        if (!soItem.getSalesOrder().getId().equals(order.getId())) {
            throw new BadRequestException("Item does not belong to this Sales Order");
        }
    }

    private SalesOrder getOrCreateSalesOrder(InvoiceCreateDto dto, Long tenantId) {
        // SCENARIO 1: Existing Sales Order
        if (dto.getSalesOrderId() != null) {
            SalesOrder so = salesOrderRepository.findByIdAndTenantId(dto.getSalesOrderId(), tenantId)
                    .orElseThrow(() -> new CommonException("Sales order not found", HttpStatus.NOT_FOUND));

            if (so.getStatus() == SalesOrderStatus.FULLY_INVOICED || so.getStatus() == SalesOrderStatus.CANCELLED) {
                throw new BadRequestException("Sales Order is already completed or cancelled");
            }
            return so;
        }

        // SCENARIO 2: Create "Direct" Sales Order (Hidden Logic)
        if (dto.getWarehouseId() == null) {
            throw new BadRequestException("Warehouse ID is required for direct invoices");
        }

        Contact contact = contactRepository.findByIdAndTenantId(dto.getCustomerId(), tenantId)
                .orElseThrow(() -> new CommonException("Customer not found", HttpStatus.NOT_FOUND));

        // A. Create Header
        SalesOrder newSo = SalesOrder.builder()
                .tenantId(tenantId)
                .warehouseId(dto.getWarehouseId())
                .customer(contact)
                .orderNumber(DocumentNumberUtil.generate(DocPrefix.SO)) // Unique prefix for Direct Bills
                .orderDate(new Date())
                .status(SalesOrderStatus.CREATED)
                .source(SalesOrderSource.DIRECT_SALES)
                .remarks("Auto-generated from Direct Invoice")
                .items(new ArrayList<>())
                .build();

        // B. Create Items & Calculate Totals
        BigDecimal subTotal = BigDecimal.ZERO;

        // We need to keep track of the DTOs to map IDs back later
        // Map<ItemMasterID, DTO> isn't safe if same item exists twice, but assuming unique items for now:
        List<SalesOrderItem> newItems = new ArrayList<>();

        for (InvoiceItemCreateDto itemDto : dto.getItems()) {
            Item itemMaster = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new CommonException("Item not found: " + itemDto.getItemId(), HttpStatus.NOT_FOUND));

            BigDecimal price = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : itemMaster.getSellingPrice();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(itemDto.getQuantity())); // Simplified calc for SO

            subTotal = subTotal.add(lineTotal);

            SalesOrderItem soItem = SalesOrderItem.builder()
                    .salesOrder(newSo)
                    .itemId(itemMaster.getId())
                    .itemName(itemMaster.getName())
                    .orderedQty(itemDto.getQuantity())
                    .invoicedQty(0) // Will be updated in main logic
                    .quantity(0)
                    .unitPrice(price)
                    .discount(itemDto.getDiscountAmount() != null ? itemDto.getDiscountAmount() : BigDecimal.ZERO)
                    .tax(itemDto.getTaxAmount() != null ? itemDto.getTaxAmount() : BigDecimal.ZERO) // Map Tax too
                    .lineTotal(lineTotal) // Note: This is rough total, Invoice logic recalculates exact
                    .build();

            newItems.add(soItem);
            newSo.getItems().add(soItem);
        }

        newSo.setSubTotal(subTotal);
        newSo.setGrandTotal(subTotal); // Rough calc

        // C. Save (Generates IDs)
        SalesOrder savedSo = salesOrderRepository.save(newSo);

        // D. CRITICAL: Inject new IDs back into the DTO
        // This tricks the rest of your code into thinking these items always existed.
        for (int i = 0; i < savedSo.getItems().size(); i++) {
            SalesOrderItem savedItem = savedSo.getItems().get(i);
            InvoiceItemCreateDto dtoItem = dto.getItems().get(i); // Relies on matching index order

            dtoItem.setSoItemId(savedItem.getId());
        }

        return savedSo;
    }

    private String deductStock(Invoice invoice, InvoiceItemCreateDto dto) {
        StockUpdateDto stockUpdate = StockUpdateDto.builder()
                .itemId(dto.getItemId())
                .warehouseId(invoice.getWarehouseId())
                .quantity(dto.getQuantity())
                .transactionType(MovementType.OUT)
                .referenceType(ReferenceType.SALE)
                .referenceId(invoice.getId())
                .batchNumber(dto.getBatchNumber())
                .build();

        // Ensure your StockService returns CommonResponse<String> or Object
        CommonResponse response = stockService.updateStock(stockUpdate);

        // Now this will work because we added the 'data' field
        if (response.getData() != null) {
            return response.getData().toString();
        }

        // Fallback: If stock service didn't return a specific batch (e.g., non-batch item),
        // use what was requested.
        return dto.getBatchNumber();
    }

    private void updateSalesOrderStatus(SalesOrder salesOrder) {
        List<SalesOrderItem> allItems = salesOrderItemRepository.findBySalesOrderId(salesOrder.getId());

        boolean allFullyInvoiced = true;
        boolean anyInvoiced = false;

        for (SalesOrderItem item : allItems) {
            if (item.getInvoicedQty() > 0) anyInvoiced = true;
            if (item.getInvoicedQty() < item.getOrderedQty()) {
                allFullyInvoiced = false;
            }
        }

        if (allFullyInvoiced) {
            salesOrder.setStatus(SalesOrderStatus.FULLY_INVOICED);
        } else if (anyInvoiced) {
            salesOrder.setStatus(SalesOrderStatus.PARTIALLY_INVOICED);
        }

        salesOrderRepository.save(salesOrder);
    }

    private void handleItemModification(Invoice invoice, InvoiceItem currentItem, InvoiceItemCreateDto dto) {
        // 1. Calculate Quantity Difference
        int oldQty = currentItem.getQuantity();
        int newQty = dto.getQuantity();
        int deltaQty = newQty - oldQty;

        // 2. Handle Stock & SO Logic based on Delta
        if (deltaQty != 0) {
            // Stock Logic
            if (deltaQty > 0) {
                // Increasing Qty -> Deduct MORE Stock (OUT)
                updateStockHelper(invoice, currentItem.getItemId(), deltaQty, MovementType.OUT, currentItem.getBatchNumber());
            } else {
                // Decreasing Qty -> Return Stock (IN)
                updateStockHelper(invoice, currentItem.getItemId(), Math.abs(deltaQty), MovementType.IN, currentItem.getBatchNumber());
            }

            // Sales Order Logic
            if (currentItem.getSoItemId() != null) {
                SalesOrderItem soItem = salesOrderItemRepository.findById(currentItem.getSoItemId())
                        .orElseThrow(() -> new BadRequestException("Linked SO Item not found"));

                // Validate limits if increasing
                if (deltaQty > 0) validateRemainingQuantity(soItem, deltaQty);

                soItem.setInvoicedQty(soItem.getInvoicedQty() + deltaQty);
                salesOrderItemRepository.save(soItem);
            }
        }

        //Update Entity Fields
        BigDecimal price = dto.getUnitPrice() != null ? dto.getUnitPrice() : currentItem.getUnitPrice();
        BigDecimal discount = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;

        // Recalculate Line Total
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(newQty)).subtract(discount).add(tax);

        currentItem.setQuantity(newQty);
        currentItem.setUnitPrice(price);
        currentItem.setDiscountAmount(discount);
        currentItem.setTaxAmount(tax);
        currentItem.setLineTotal(lineTotal);
        currentItem.setBatchNumber(dto.getBatchNumber()); // Important if batch changed
    }

    //HELPER Delete Item
    private void handleItemDeletion(Invoice invoice, InvoiceItem item) {
        // 1. Return Stock (IN)
        updateStockHelper(invoice, item.getItemId(), item.getQuantity(), MovementType.IN, item.getBatchNumber());

        // 2. Revert Sales Order Qty
        if (item.getSoItemId() != null) {
            SalesOrderItem soItem = salesOrderItemRepository.findById(item.getSoItemId()).orElse(null);
            if (soItem != null) {
                soItem.setInvoicedQty(soItem.getInvoicedQty() - item.getQuantity());
                salesOrderItemRepository.save(soItem);
            }
        }
    }

    //Create New Item (Similar to Create Method)
    private void handleItemCreation(Invoice invoice, InvoiceItemCreateDto dto) {
        // 1. Fetch & Validate
        SalesOrderItem soItem = null;
        if (dto.getSoItemId() != null) {
            soItem = salesOrderItemRepository.findById(dto.getSoItemId())
                    .orElseThrow(() -> new BadRequestException("Invalid SO Item ID"));
            validateRemainingQuantity(soItem, dto.getQuantity());

            // Update SO
            soItem.setInvoicedQty(soItem.getInvoicedQty() + dto.getQuantity());
            salesOrderItemRepository.save(soItem);
        }

        Item itemMaster = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new BadRequestException("Item not found"));

        // 2. Calculate Financials
        BigDecimal price = dto.getUnitPrice() != null ? dto.getUnitPrice() : (soItem != null ? soItem.getUnitPrice() : itemMaster.getSellingPrice());
        BigDecimal discount = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(dto.getQuantity())).subtract(discount).add(tax);

        // 3. Create Entity
        InvoiceItem newItem = InvoiceItem.builder()
                .invoice(invoice)
                .soItemId(soItem != null ? soItem.getId() : null)
                .itemId(itemMaster.getId())
                .itemName(itemMaster.getName())
                .sku(itemMaster.getSku())
                .batchNumber(dto.getBatchNumber())
                .quantity(dto.getQuantity())
                .unitPrice(price)
                .discountAmount(discount)
                .taxAmount(tax)
                .lineTotal(lineTotal)
                .build();

        invoice.getItems().add(newItem);

        // 4. Deduct Stock (OUT)
        updateStockHelper(invoice, newItem.getItemId(), newItem.getQuantity(), MovementType.OUT, newItem.getBatchNumber());
    }

    //UTILITY: Centralized Stock Update
    private void updateStockHelper(Invoice invoice, Long itemId, int qty, MovementType type, String batch) {
        StockUpdateDto stockUpdate = StockUpdateDto.builder()
                .itemId(itemId)
                .warehouseId(invoice.getWarehouseId())
                .quantity(qty)
                .transactionType(type) // IN (Return) or OUT (Deduct)
                .referenceType(ReferenceType.SALE)
                .referenceId(invoice.getId())
                .batchNumber(batch)
                .build();
        stockService.updateStock(stockUpdate);
    }

    public InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceItemDto> itemDtos = invoice.getItems().stream()
                .map(item -> InvoiceItemDto.builder()
                        .id(item.getId())
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .sku(item.getSku())
                        .batchNumber(item.getBatchNumber())
                        .quantity(item.getQuantity())
                        .discountAmount(item.getDiscountAmount()) // Add this
                        .taxAmount(item.getTaxAmount())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        ContactMiniDto contactMini = ContactMiniDto
                .builder()
                .id(invoice.getCustomer().getId())
                .contactCode(invoice.getCustomer().getContactCode())
                .name(invoice.getCustomer().getName())
                .build();

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .salesOrderId(invoice.getSalesOrder() != null ? invoice.getSalesOrder().getId() : null)
                .contactMini(contactMini)
                .customerId(invoice.getCustomer().getId())
                .customerName(invoice.getCustomer().getName())
                .invoiceDate(invoice.getInvoiceDate())
                .totalDiscount(invoice.getTotalDiscount()) // Add this
                .totalTax(invoice.getTotalTax())           // Add this
                .status(invoice.getStatus())
                .deliveryStatus(invoice.getDeliveryStatus())
                .paymentStatus(invoice.getPaymentStatus())
                .subTotal(invoice.getSubTotal())
                .amountPaid(invoice.getAmountPaid())
                .grandTotal(invoice.getGrandTotal())
                .balance(invoice.getBalance())
                .items(itemDtos)
                .build();
    }

    private String generateInvoiceNumber() {
        int random = new Random().nextInt(9000) + 1000;
        return "INV-" + System.currentTimeMillis() + "-" + random;
    }
}