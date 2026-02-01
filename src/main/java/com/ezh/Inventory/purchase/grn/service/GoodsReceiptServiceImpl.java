package com.ezh.Inventory.purchase.grn.service;

import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.purchase.grn.dto.GrnDto;
import com.ezh.Inventory.purchase.grn.dto.GrnItemDto;
import com.ezh.Inventory.purchase.grn.entity.GoodsReceipt;
import com.ezh.Inventory.purchase.grn.entity.GoodsReceiptItem;
import com.ezh.Inventory.purchase.grn.entity.GrnStatus;
import com.ezh.Inventory.purchase.grn.repository.GoodsReceiptItemRepository;
import com.ezh.Inventory.purchase.grn.repository.GoodsReceiptRepository;
import com.ezh.Inventory.purchase.po.entity.PoStatus;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrder;
import com.ezh.Inventory.purchase.po.entity.PurchaseOrderItem;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderItemRepository;
import com.ezh.Inventory.purchase.po.repository.PurchaseOrderRepository;
import com.ezh.Inventory.stock.dto.StockUpdateDto;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.ReferenceType;
import com.ezh.Inventory.stock.entity.StockBatch;
import com.ezh.Inventory.stock.repository.StockBatchRepository;
import com.ezh.Inventory.stock.service.StockService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private final GoodsReceiptRepository grnRepository;
    private final GoodsReceiptItemRepository grnItemRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockService stockService;
    private final ItemRepository itemRepository;

    public CommonResponse createAndApproveGrn(GrnDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch PO
        PurchaseOrder po = poRepository.findById(dto.getPurchaseOrderId())
                .orElseThrow(() -> new CommonException("Invalid PO ID", HttpStatus.BAD_REQUEST));

        // 2. Create GRN Header
        GoodsReceipt grn = GoodsReceipt.builder()
                .tenantId(tenantId)
                .purchaseOrderId(po.getId())
                .grnNumber("GRN-" + System.currentTimeMillis())
                .receivedDate(System.currentTimeMillis())
                .supplierInvoiceNo(dto.getSupplierInvoiceNo())
                .grnStatus(GrnStatus.APPROVED)
                .build();
        grnRepository.save(grn);

        List<GoodsReceiptItem> grnItems = new ArrayList<>();
        boolean isPoFullyReceived = true;

        // 3. Process Items
        for (GrnItemDto itemDto : dto.getItems()) {

            PurchaseOrderItem poItem = poItemRepository.findById(itemDto.getPoItemId())
                    .orElseThrow(() -> new CommonException("Invalid PO Item ID", HttpStatus.BAD_REQUEST));

            int acceptedQty = itemDto.getReceivedQty() - itemDto.getRejectedQty();

            // Save GRN Item
            GoodsReceiptItem grnItem = GoodsReceiptItem.builder()
                    .goodsReceiptId(grn.getId())
                    .poItemId(poItem.getId())
                    .itemId(itemDto.getItemId())
                    .receivedQty(itemDto.getReceivedQty())
                    .rejectedQty(itemDto.getRejectedQty())
                    .acceptedQty(acceptedQty)
                    .batchNumber(itemDto.getBatchNumber())
                    .expiryDate(itemDto.getExpiryDate())
                    .build();
            grnItems.add(grnItem);

            if (acceptedQty > 0) {

                // --- STEP A: Create the Batch Record (Crucial for Tracking) ---
                if (itemDto.getBatchNumber() != null && !itemDto.getBatchNumber().isEmpty()) {
                    StockBatch batch = StockBatch.builder()
                            .itemId(itemDto.getItemId())
                            .warehouseId(po.getWarehouseId())
                            .tenantId(tenantId)
                            .batchNumber(itemDto.getBatchNumber())
                            .grnId(grn.getId())
                            .buyPrice(poItem.getUnitPrice()) // Save Specific Cost
                            .initialQty(acceptedQty)
                            .remainingQty(acceptedQty)
                            .expiryDate(itemDto.getExpiryDate())
                            .build();
                    stockBatchRepository.save(batch);
                }

                // --- STEP B: Update Master Stock ---
                StockUpdateDto stockUpdate = StockUpdateDto.builder()
                        .itemId(itemDto.getItemId())
                        .warehouseId(po.getWarehouseId())
                        .quantity(acceptedQty)
                        .transactionType(MovementType.IN)
                        .referenceType(ReferenceType.GRN)
                        .referenceId(grn.getId())
                        .unitPrice(poItem.getUnitPrice())
                        .batchNumber(itemDto.getBatchNumber()) // <--- PASSED HERE
                        .build();

                stockService.updateStock(stockUpdate);
            }

            // Update PO Item Progress
            poItem.setReceivedQty(poItem.getReceivedQty() + acceptedQty);
            poItemRepository.save(poItem);

            if (poItem.getReceivedQty() < poItem.getOrderedQty()) {
                isPoFullyReceived = false;
            }
        }

        grnItemRepository.saveAll(grnItems);

        // 4. Update PO Header Status
        if (isPoFullyReceived) {
            po.setPoStatus(PoStatus.COMPLETED);
        } else {
            po.setPoStatus(PoStatus.PARTIALLY_RECEIVED);
        }
        poRepository.save(po);

        return CommonResponse.builder().id(grn.getId().toString()).message("GRN Created & Stock Updated").build();
    }


    @Override
    @Transactional(readOnly = true)
    public GrnDto getGrnDetails(Long grnId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        GoodsReceipt grn = grnRepository.findByIdAndTenantId(grnId, tenantId)
                .orElseThrow(() -> new CommonException("GRN not found", HttpStatus.BAD_REQUEST));

        List<GoodsReceiptItem> items = grnItemRepository.findByGoodsReceiptId(grnId);

        return mapToGrnDto(grn, items);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<GrnDto> getAllGrns(Integer page, Integer size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<GoodsReceipt> receipts = grnRepository.findByTenantId(tenantId, pageable);

        return receipts.map(grn -> {
            List<GoodsReceiptItem> items = grnItemRepository.findByGoodsReceiptId(grn.getId());
            return mapToGrnDto(grn, items);
        });
    }


    @Override
    @Transactional(readOnly = true)
    public List<GrnDto> getGrnHistoryForPo(Long purchaseOrderId) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<GoodsReceipt> grns = grnRepository.findByPurchaseOrderIdAndTenantId(purchaseOrderId, tenantId);

        return grns.stream().map(grn -> {
            List<GoodsReceiptItem> items = grnItemRepository.findByGoodsReceiptId(grn.getId());
            return mapToGrnDto(grn, items);
        }).collect(Collectors.toList());
    }

    private GrnDto mapToGrnDto(GoodsReceipt grn, List<GoodsReceiptItem> items) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        PurchaseOrder purchaseOrder = poRepository.findByIdAndTenantId(grn.getPurchaseOrderId(), tenantId)
                .orElseThrow(() -> new CommonException("PO not found", HttpStatus.BAD_REQUEST));

        List<Long> itemIds = items.stream()
                .map(GoodsReceiptItem::getItemId)
                .distinct()
                .toList();

        Map<Long, String> itemNamesMap = itemRepository.findByIdIn(itemIds)
                .stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        return GrnDto.builder()
                .id(grn.getId())
                .supplierId(purchaseOrder.getSupplierId())
                .supplierName(purchaseOrder.getSupplierName())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrderId())
                .supplierInvoiceNo(grn.getSupplierInvoiceNo())
                .status(grn.getGrnStatus())
                .createdAt(grn.getCreatedAt())
                .items(
                        items.stream()
                                .map(item -> mapToGrnItemDto(item, itemNamesMap))
                                .toList()
                )
                .build();
    }

    private GrnItemDto mapToGrnItemDto(GoodsReceiptItem item, Map<Long, String> itemNamesMap) {
        return GrnItemDto.builder()
                .poItemId(item.getPoItemId())
                .itemId(item.getItemId())
                .itemName(itemNamesMap.getOrDefault(item.getItemId(), "Unknown Item"))
                .receivedQty(item.getReceivedQty())
                .rejectedQty(item.getRejectedQty())
                .batchNumber(item.getBatchNumber())
                .expiryDate(item.getExpiryDate())
                .build();
    }

}
