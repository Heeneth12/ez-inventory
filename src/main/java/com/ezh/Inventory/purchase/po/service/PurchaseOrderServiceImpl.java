package com.ezh.Inventory.purchase.po.service;

import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.notifications.entity.NotificationType;
import com.ezh.Inventory.notifications.service.NotificationService;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderFilter;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderItemDto;
import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrderItem;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderItemRepository;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderRepository;
import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestDto;
import com.ezh.Inventory.purchase.prq.dto.PurchaseRequestItemDto;
import com.ezh.Inventory.purchase.prq.entity.PrqStatus;
import com.ezh.Inventory.purchase.prq.service.PurchaseRequestService;
import com.ezh.Inventory.security.UserContext;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.DocPrefix;
import com.ezh.Inventory.utils.common.DocumentNumberUtil;
import com.ezh.Inventory.utils.common.client.AuthServiceClient;
import com.ezh.Inventory.utils.common.dto.UserMiniDto;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final PurchaseRequestService purchaseRequestService;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;
    private final AuthServiceClient authServiceClient;
    private final UserContext userContext;

    @Override
    @Transactional
    public CommonResponse<?> createPurchaseOrder(PurchaseOrderDto dto) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Long currentUserId = UserContextUtil.getUserIdOrThrow();
        Long prqId = dto.getPrqId();

        if (prqId == null) {
            CommonResponse<?> prqResponse = purchaseRequestService.createPrq(mapToPrqDto(dto));
            prqId = Long.parseLong(prqResponse.getId());
        }

        // 1. CAPTURE INPUTS (The "Flat" values)
        BigDecimal flatDiscount = safeDecimal(dto.getFlatDiscount());
        BigDecimal flatTax = safeDecimal(dto.getFlatTax());

        // 2. Initialize PO
        PurchaseOrder po = PurchaseOrder.builder()
                .tenantId(tenantId)
                .vendorId(currentUserId)
                .warehouseId(dto.getWarehouseId())
                .purchaseRequestId(prqId)
                .orderNumber(DocumentNumberUtil.generate(DocPrefix.PO))
                .orderDate(System.currentTimeMillis())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .poStatus(PoStatus.PENDING)
                .notes(dto.getNotes())
                .flatDiscount(flatDiscount)
                .flatTax(flatTax)
                .build();

        po = poRepository.save(po);

        // 3. AGGREGATION VARIABLES
        BigDecimal sumLineTotals = BigDecimal.ZERO;
        BigDecimal sumItemDiscounts = BigDecimal.ZERO;
        BigDecimal sumItemTaxes = BigDecimal.ZERO;

        List<PurchaseOrderItem> itemsToSave = new ArrayList<>();

        if (dto.getItems() != null) {
            for (PurchaseOrderItemDto itemDto : dto.getItems()) {

                BigDecimal qty = BigDecimal.valueOf(itemDto.getOrderedQty() != null ? itemDto.getOrderedQty() : 1);
                BigDecimal price = safeDecimal(itemDto.getUnitPrice());

                // Item Level Inputs
                BigDecimal itemDisc = safeDecimal(itemDto.getDiscount());
                BigDecimal itemTax = safeDecimal(itemDto.getTax());

                // Track Sums for Reporting
                sumItemDiscounts = sumItemDiscounts.add(itemDisc);
                sumItemTaxes = sumItemTaxes.add(itemTax);

                // CALC 1: Item Line Total
                // Formula: (Qty * Price) - ItemDisc + ItemTax
                BigDecimal baseAmount = price.multiply(qty);
                BigDecimal lineTotal = baseAmount.subtract(itemDisc).add(itemTax);

                // Validation: No negative lines
                if (lineTotal.compareTo(BigDecimal.ZERO) < 0) lineTotal = BigDecimal.ZERO;

                // Add to Subtotal
                sumLineTotals = sumLineTotals.add(lineTotal);

                // Build Item
                PurchaseOrderItem item = PurchaseOrderItem.builder()
                        .purchaseOrderId(po.getId())
                        .itemId(itemDto.getItemId())
                        .orderedQty(itemDto.getOrderedQty())
                        .receivedQty(0)
                        .unitPrice(price)
                        .discount(itemDisc)
                        .tax(itemTax)
                        .lineTotal(lineTotal)
                        .build();
                itemsToSave.add(item);
            }
        }

        poItemRepository.saveAll(itemsToSave);

        // Total Discount = All Item Discounts + Flat Discount
        po.setTotalDiscount(sumItemDiscounts.add(flatDiscount));

        // Total Tax = All Item Taxes + Flat Tax
        po.setTotalTax(sumItemTaxes.add(flatTax));

        // Total Amount = Sum of all Line Totals (Subtotal)
        po.setTotalAmount(sumLineTotals);

        // 5. CALCULATE GRAND TOTAL (Final Payable)
        // Formula: Subtotal - Flat Discount + Flat Tax
        BigDecimal grandTotal = sumLineTotals
                .subtract(flatDiscount)
                .add(flatTax);

        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;
        po.setGrandTotal(grandTotal);
        poRepository.save(po);
        purchaseRequestService.updateStatus(prqId, PrqStatus.CONVERTED);
        return CommonResponse.builder()
                .id(po.getId().toString())
                .message("PO Created. Pay: " + grandTotal)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long id) throws CommonException {
        PurchaseOrder po = poRepository.findByIdAndTenantId(id, UserContextUtil.getTenantIdOrThrow())
                .orElseThrow(() -> new CommonException("Purchase Order not found", HttpStatus.BAD_REQUEST));

        List<PurchaseOrderItem> items = poItemRepository.findByPurchaseOrderId(id);

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(List.of(po.getVendorId()));

        return mapToDto(po, items, vendorDetails, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getAllPurchaseOrders(Integer page, Integer size, PurchaseOrderFilter filter) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Long vendorId = null;
        if (Objects.equals(userContext.getUserType(), "VENDOR")) {
            vendorId = userContext.getUserId();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PurchaseOrder> poPage = poRepository.findAllPurchaseOrders(
                tenantId,
                filter.getId(),
                filter.getStatus() != null ? PoStatus.valueOf(filter.getStatus()) : null,
                vendorId,
                filter.getWarehouseId(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        Set<Long> vendorIds = poPage.stream().map(PurchaseOrder::getVendorId).collect(Collectors.toSet());

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(vendorIds.stream().toList());

        return poPage.map(po -> mapToDto(po, Collections.emptyList(), vendorDetails, false));
    }


    @Override
    @Transactional
    public CommonResponse updatePurchaseOrder(Long poId, PurchaseOrderDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        PurchaseOrder po = poRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new CommonException("PO not found", HttpStatus.BAD_REQUEST));

        // VALIDATION: Can only edit if Draft or Issued (and nothing received yet)
        if (po.getPoStatus() == PoStatus.PARTIALLY_RECEIVED || po.getPoStatus() == PoStatus.COMPLETED) {
            throw new CommonException("Cannot edit a PO that has already received goods.", HttpStatus.BAD_REQUEST);
        }

        // 1. Update Header
        po.setWarehouseId(dto.getWarehouseId());
        po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        po.setNotes(dto.getNotes());

        // 2. Update Items (Strategy: Delete old, Insert new for simplicity in Drafts)
        // Ideally, you should compare IDs to update existing rows, but this is safer for consistency.
        poItemRepository.deleteByPurchaseOrderId(po.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderItem> newItems = new ArrayList<>();

        for (PurchaseOrderItemDto itemDto : dto.getItems()) {
            BigDecimal lineTotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getOrderedQty()));
            totalAmount = totalAmount.add(lineTotal);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrderId(po.getId())
                    .itemId(itemDto.getItemId())
                    .orderedQty(itemDto.getOrderedQty())
                    .receivedQty(0)
                    .unitPrice(itemDto.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();
            newItems.add(item);
        }

        poItemRepository.saveAll(newItems);
        po.setTotalAmount(totalAmount);
        poRepository.save(po);

        return CommonResponse.builder().id(String.valueOf(po.getId())).message("Purchase Order Updated").build();
    }

    @Override
    @Transactional
    public CommonResponse updatePurchaseOrderStatus(Long poId, PoStatus newStatus) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        PurchaseOrder po = poRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new CommonException("PO not found", HttpStatus.BAD_REQUEST));

        // Logic for Cancellation
        if (newStatus == PoStatus.CANCELLED) {
            long receivedCount = poItemRepository.countReceivedItemsForPo(poId);
            if (receivedCount > 0) {
                throw new CommonException("Cannot cancel PO. Goods have already been received.", HttpStatus.BAD_REQUEST);
            }
        }

        if (po.getPoStatus() == PoStatus.PENDING && newStatus == PoStatus.ASN_CONFIRMED) {
            notificationService.sendToOrg(
                    po.getTenantId().toString(),
                    "ASN Confirmed: PO #" + po.getOrderNumber(),
                    "Advanced Shipping Notice (ASN) has been confirmed. The goods for PO #"
                            + po.getOrderNumber() + " are now in transit and ready for delivery.",
                    NotificationType.INFO
            );
        }
        // Update to the requested status
        po.setPoStatus(newStatus);
        poRepository.save(po);

        return CommonResponse.builder()
                .id(String.valueOf(po.getId()))
                .message("Purchase Order status updated to " + newStatus)
                .build();
    }

    // Mapper Helper
    private PurchaseOrderDto mapToDto(PurchaseOrder po, List<PurchaseOrderItem> items,
                                      Map<Long, UserMiniDto> vendorDetails, boolean passItems) {

        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setVendorId(po.getVendorId());
        dto.setVendorDetails(vendorDetails.get(po.getVendorId()));
        dto.setWarehouseId(po.getWarehouseId());
        dto.setOrderNumber(po.getOrderNumber());
        dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        dto.setStatus(po.getPoStatus());
        dto.setNotes(po.getNotes());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCreatedAt(po.getCreatedAt());

        // Only map items if allowed
        if (passItems && items != null) {
            List<Long> itemIds = items.stream()
                    .map(PurchaseOrderItem::getItemId)
                    .distinct()
                    .toList();

            Map<Long, String> itemNamesMap = itemRepository.findByIdIn(itemIds)
                    .stream()
                    .collect(Collectors.toMap(Item::getId, Item::getName));

            List<PurchaseOrderItemDto> itemDtos = items.stream()
                    .map(item -> PurchaseOrderItemDto.builder()
                            .id(item.getId())
                            .itemId(item.getItemId())
                            .itemName(itemNamesMap.getOrDefault(item.getItemId(), "Unknown Item"))
                            .orderedQty(item.getOrderedQty())
                            .unitPrice(item.getUnitPrice())
                            .build()
                    ).toList();

            dto.setItems(itemDtos);

        } else {
            dto.setItems(Collections.emptyList());
        }

        return dto;
    }

    private BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private PurchaseRequestDto mapToPrqDto(PurchaseOrderDto poDto) {
        if (poDto == null) return null;

        return PurchaseRequestDto.builder()
                .vendorId(poDto.getVendorId())
                .warehouseId(poDto.getWarehouseId())
                .notes(poDto.getNotes())
                .status(PrqStatus.PENDING)
                .items(poDto.getItems() != null ?
                        poDto.getItems().stream()
                                .map(this::mapToPrqItemDto)
                                .collect(Collectors.toList()) : new ArrayList<>())
                .totalEstimatedAmount(poDto.getTotalAmount())
                .build();
    }

    private PurchaseRequestItemDto mapToPrqItemDto(PurchaseOrderItemDto poItemDto) {
        if (poItemDto == null) return null;
        BigDecimal qty = BigDecimal.valueOf(poItemDto.getOrderedQty() != null ? poItemDto.getOrderedQty() : 0);
        BigDecimal price = poItemDto.getUnitPrice() != null ? poItemDto.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineTotal = qty.multiply(price);

        return PurchaseRequestItemDto.builder()
                .itemId(poItemDto.getItemId())
                .itemName(poItemDto.getItemName())
                .requestedQty(poItemDto.getOrderedQty())
                .estimatedUnitPrice(poItemDto.getUnitPrice())
                .lineTotal(lineTotal)
                .build();
    }
}
