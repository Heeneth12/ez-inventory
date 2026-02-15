package com.ezh.Inventory.purchase.grn.service;

import com.ezh.Inventory.items.entity.Item;
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
import com.ezh.Inventory.purchase.returns.entity.PurchaseReturnItem;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final AuthServiceClient authServiceClient;

    public CommonResponse createAndApproveGrn(GrnDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch PO
        PurchaseOrder po = poRepository.findById(dto.getPurchaseOrderId())
                .orElseThrow(() -> new CommonException("Invalid PO ID", HttpStatus.BAD_REQUEST));

        // 2. Create GRN Header
        GoodsReceipt grn = GoodsReceipt.builder()
                .tenantId(tenantId)
                .purchaseOrderId(po.getId())
                .grnNumber(DocumentNumberUtil.generate(DocPrefix.GRN))
                .receivedDate(System.currentTimeMillis())
                .supplierInvoiceNo(dto.getSupplierInvoiceNo())
                .grnStatus(GrnStatus.RECEIVED)
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

        // Fetch GRN with all related data using JOIN FETCH (single optimized query)
        GoodsReceipt grn = grnRepository.findByIdWithAllRelations(grnId, tenantId)
                .orElseThrow(() -> new CommonException("GRN not found", HttpStatus.BAD_REQUEST));

        // Fetch vendor details if needed
        Map<Long, UserMiniDto> vendorDetails = Map.of();
        if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getVendorId() != null) {
            vendorDetails = authServiceClient.getBulkUserDetails(List.of(grn.getPurchaseOrder().getVendorId()));
        }

        return mapToGrnDto(grn, grn.getItems(), vendorDetails);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<GrnDto> getAllGrns(Integer page, Integer size) throws CommonException {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<GoodsReceipt> receipts = grnRepository.findByTenantId(tenantId, pageable);

        // Batch fetch all related data using IN clause (much better than loop)
        List<Long> grnIds = receipts.getContent().stream()
                .map(GoodsReceipt::getId)
                .toList();

        if (!grnIds.isEmpty()) {
            // Fetch all GRNs with their relations in a single batch query
            grnRepository.findAllByIdWithRelations(grnIds, tenantId);
        }

        Set<Long> vendorIds = receipts.getContent().stream()
                .filter(grn -> grn.getPurchaseOrder() != null)
                .map(grn -> grn.getPurchaseOrder().getVendorId())
                .collect(Collectors.toSet());

        Map<Long, UserMiniDto> finalVendorDetails = Map.of();
        if (!vendorIds.isEmpty()) {
            finalVendorDetails = authServiceClient.getBulkUserDetails(vendorIds.stream().toList());
        }

        Map<Long, UserMiniDto> vendorDetailsMap = finalVendorDetails;
        return receipts.map(grn -> mapToGrnDto(grn, grn.getItems(), vendorDetailsMap));
    }


    @Override
    @Transactional(readOnly = true)
    public List<GrnDto> getGrnHistoryForPo(Long purchaseOrderId) {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        List<GoodsReceipt> grns = grnRepository.findByPurchaseOrderIdAndTenantId(purchaseOrderId, tenantId);

        // Batch fetch all related data
        List<Long> grnIds = grns.stream().map(GoodsReceipt::getId).toList();
        
        if (!grnIds.isEmpty()) {
            grnRepository.findAllByIdWithRelations(grnIds, tenantId);
        }

        // Fetch vendor details for all GRNs
        Set<Long> vendorIds = grns.stream()
                .filter(grn -> grn.getPurchaseOrder() != null)
                .map(grn -> grn.getPurchaseOrder().getVendorId())
                .collect(Collectors.toSet());

        Map<Long, UserMiniDto> vendorDetails = Map.of();
        if (!vendorIds.isEmpty()) {
            vendorDetails = authServiceClient.getBulkUserDetails(vendorIds.stream().toList());
        }

        Map<Long, UserMiniDto> finalVendorDetails = vendorDetails;
        return grns.stream().map(grn -> mapToGrnDto(grn, grn.getItems(), finalVendorDetails)).toList();
    }

    private GrnDto mapToGrnDto(GoodsReceipt grn, Set<GoodsReceiptItem> items, Map<Long, UserMiniDto> vendorDetails) {
        // All data is already loaded via JOIN FETCH, no additional queries needed!
        // Build item name map from preloaded Item relationships
        Map<Long, Item> itemsMap = items.stream()
                .filter(gri -> gri.getItem() != null)
                .collect(Collectors.toMap(
                        GoodsReceiptItem::getItemId,
                        GoodsReceiptItem::getItem
                ));

        // Flatten all return items from preloaded PurchaseReturn relationships
        List<PurchaseReturnItem> returnItems = grn.getPurchaseReturns().stream()
                .flatMap(pr -> pr.getPurchaseReturnItems().stream())
                .toList();

        // Group return items by itemId + batchNumber composite key
        Map<String, List<PurchaseReturnItem>> returnItemMap = returnItems.stream()
                .collect(Collectors.groupingBy(returnItem -> 
                    returnItem.getItemId() + "_" + (returnItem.getBatchNumber() != null ? returnItem.getBatchNumber() : "")));

        return GrnDto.builder()
                .id(grn.getId())
                .vendorId(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getVendorId() : null)
                .vendorDetails(grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getVendorId() != null 
                    ? vendorDetails.get(grn.getPurchaseOrder().getVendorId()) 
                    : null)
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrderId())
                .purchaseOrderNumber(grn.getPurchaseOrder().getOrderNumber())
                .supplierInvoiceNo(grn.getSupplierInvoiceNo())
                .status(grn.getGrnStatus())
                .createdAt(grn.getCreatedAt())
                .items(
                        items.stream()
                                .map(item -> mapToGrnItemDto(item, itemsMap, returnItemMap))
                                .toList()
                )
                .build();
    }

    private GrnItemDto mapToGrnItemDto(GoodsReceiptItem item, Map<Long, Item> itemsMap,
                                       Map<String, List<PurchaseReturnItem>> returnItemMap) {
        // Create composite key to lookup return items for this specific item + batch
        String compositeKey = item.getItemId() + "_" + (item.getBatchNumber() != null ? item.getBatchNumber() : "");
        
        // Calculate total returned quantity for this item + batch combination
        int totalReturnedQty = returnItemMap.getOrDefault(compositeKey, List.of())
                .stream()
                .mapToInt(PurchaseReturnItem::getReturnQty)
                .sum();
        
        // Calculate unit price from PO line total
        BigDecimal unitPrice = null;
        if (item.getPoItem() != null && item.getPoItem().getLineTotal() != null && item.getReceivedQty() > 0) {
            unitPrice = item.getPoItem().getLineTotal().divide(
                BigDecimal.valueOf(item.getReceivedQty()), 
                2, 
                RoundingMode.HALF_UP
            );
        }
        
        return GrnItemDto.builder()
                .poItemId(item.getPoItemId())
                .poItemPrice(unitPrice)
                .itemId(item.getItemId())
                .itemName(itemsMap.get(item.getItemId()) != null ? itemsMap.get(item.getItemId()).getName() : "Unknown Item")
                .receivedQty(item.getReceivedQty())
                .rejectedQty(item.getRejectedQty())
                .returnedQty(totalReturnedQty)
                .batchNumber(item.getBatchNumber())
                .expiryDate(item.getExpiryDate())
                .build();
    }

}
