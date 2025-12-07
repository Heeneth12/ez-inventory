package com.ezh.Inventory.purchase.po.service;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.purchase.po.dto.PurchaseOrderDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public CommonResponse createPurchaseOrder(PurchaseOrderDto dto) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Contact contact = contactRepository.findByIdAndTenantId(dto.getSupplierId(),tenantId)
                .orElseThrow(() -> new  CommonException("", HttpStatus.BAD_REQUEST));

        // 1. Create Header
        PurchaseOrder po = PurchaseOrder.builder()
                .tenantId(tenantId)
                .supplierId(contact.getId())
                .supplierName(contact.getName())
                .warehouseId(dto.getWarehouseId())
                .orderNumber("PO-" + System.currentTimeMillis()) // Replace with sequence generator
                .orderDate(System.currentTimeMillis())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .status(PoStatus.ISSUED) // Or DRAFT if you want an approval step
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
    public Page<PurchaseOrderDto> getAllPurchaseOrders(Integer page, Integer size) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PurchaseOrder> poPage = poRepository.findAllByTenantId(tenantId, pageable);

        return poPage.map(po -> mapToDto(po, Collections.emptyList(), false));
    }


    @Override
    @Transactional
    public CommonResponse updatePurchaseOrder(Long poId, PurchaseOrderDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        PurchaseOrder po = poRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new CommonException("PO not found", HttpStatus.BAD_REQUEST));

        // VALIDATION: Can only edit if Draft or Issued (and nothing received yet)
        if (po.getStatus() == PoStatus.PARTIALLY_RECEIVED || po.getStatus() == PoStatus.COMPLETED) {
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
    public CommonResponse cancelPurchaseOrder(Long poId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        PurchaseOrder po = poRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new CommonException("PO not found", HttpStatus.BAD_REQUEST));


        // VALIDATION: Cannot cancel if goods received
        long receivedCount = poItemRepository.countReceivedItemsForPo(poId); // Custom Query needed
        // OR check header items:
        // boolean hasReceived = poItemRepository.findByPurchaseOrderId(poId).stream().anyMatch(i -> i.getReceivedQty() > 0);

        if (receivedCount > 0) {
            throw new CommonException("Cannot cancel PO. Goods have already been received. Use Purchase Return instead.", HttpStatus.BAD_REQUEST);
        }

        po.setStatus(PoStatus.CANCELLED);
        poRepository.save(po);

        return CommonResponse.builder().id(String.valueOf(po.getId())).message("Purchase Order Cancelled").build();
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
        dto.setStatus(po.getStatus());
        dto.setNotes(po.getNotes());
        dto.setTotalAmount(po.getTotalAmount());

        // Only map items if allowed
        if (passItems && items != null) {
            List<PurchaseOrderItemDto> itemDtos = items.stream()
                    .map(item -> PurchaseOrderItemDto.builder()
                            .itemId(item.getItemId())
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
