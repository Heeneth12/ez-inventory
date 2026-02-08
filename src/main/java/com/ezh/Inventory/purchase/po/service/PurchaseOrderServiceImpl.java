package com.ezh.Inventory.purchase.po.service;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderFilter;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderItemDto;
import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrderItem;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderItemRepository;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final ContactRepository contactRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public CommonResponse createPurchaseOrder(PurchaseOrderDto dto) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Contact contact = contactRepository.findByIdAndTenantId(dto.getSupplierId(), tenantId)
                .orElseThrow(() -> new CommonException("", HttpStatus.BAD_REQUEST));

        // 1. Create Header
        PurchaseOrder po = PurchaseOrder.builder()
                .tenantId(tenantId)
                .supplierId(contact.getId())
                .supplierName(contact.getName())
                .warehouseId(dto.getWarehouseId())
                .orderNumber("PO-" + System.currentTimeMillis()) // Replace with sequence generator
                .orderDate(System.currentTimeMillis())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .poStatus(dto.getStatus() != null ? dto.getStatus() : PoStatus.PENDING)
                .notes(dto.getNotes())
                .build();

        poRepository.save(po);

        // 2. Create Items and Calc Total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();

        for (PurchaseOrderItemDto itemDto : dto.getItems()) {
            BigDecimal lineTotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getOrderedQty()));
            totalAmount = totalAmount.add(lineTotal);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrderId(po.getId())
                    .itemId(itemDto.getItemId())
                    .orderedQty(itemDto.getOrderedQty())
                    .receivedQty(0) // Initially 0
                    .unitPrice(itemDto.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();
            items.add(item);
        }

        poItemRepository.saveAll(items);

        // Update total in header
        po.setTotalAmount(totalAmount);
        poRepository.save(po);

        return CommonResponse.builder().id(po.getId().toString()).message("PO Created").build();
    }


    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long id) throws CommonException {
        PurchaseOrder po = poRepository.findByIdAndTenantId(id, UserContextUtil.getTenantIdOrThrow())
                .orElseThrow(() -> new CommonException("Purchase Order not found", HttpStatus.BAD_REQUEST));

        List<PurchaseOrderItem> items = poItemRepository.findByPurchaseOrderId(id);

        return mapToDto(po, items, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getAllPurchaseOrders(Integer page, Integer size, PurchaseOrderFilter filter) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PurchaseOrder> poPage = poRepository.findAllPurchaseOrders(
                tenantId,
                filter.getId(),
                filter.getStatus() != null ? PoStatus.valueOf(filter.getStatus()) : null,
                filter.getSupplierId(),
                filter.getWarehouseId(),
                filter.getSearchQuery(),
                filter.getStartDateTime(),
                filter.getEndDateTime(),
                pageable
        );

        return poPage.map(po -> mapToDto(po, Collections.emptyList(), false));
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
        po.setSupplierId(dto.getSupplierId());
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

        // Update to the requested status
        po.setPoStatus(newStatus);
        poRepository.save(po);

        return CommonResponse.builder()
                .id(String.valueOf(po.getId()))
                .message("Purchase Order status updated to " + newStatus)
                .build();
    }

    // Mapper Helper
    private PurchaseOrderDto mapToDto(PurchaseOrder po, List<PurchaseOrderItem> items, boolean passItems) {

        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setSupplierId(po.getSupplierId());
        dto.setSupplierName(po.getSupplierName());
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

}
