package com.ezh.Inventory.purchase.returns.service;

import com.ezh.Inventory.purchase.returns.dto.PurchaseReturnDto;
import com.ezh.Inventory.purchase.returns.dto.PurchaseReturnItemDto;
import com.ezh.Inventory.purchase.returns.entity.PurchaseReturn;
import com.ezh.Inventory.purchase.returns.entity.PurchaseReturnItem;
import com.ezh.Inventory.purchase.returns.entity.ReturnStatus;
import com.ezh.Inventory.purchase.returns.repository.PurchaseReturnItemRepository;
import com.ezh.Inventory.purchase.returns.repository.PurchaseReturnRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseReturnServiceImpl implements PurchaseReturnService {

    private final PurchaseReturnRepository returnRepository;
    private final PurchaseReturnItemRepository returnItemRepository;
    private final StockService stockService;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public CommonResponse<?> createPurchaseReturn(PurchaseReturnDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        if (dto.getWarehouseId() == null) {
            throw new CommonException("Warehouse ID is required", HttpStatus.BAD_REQUEST);
        }

        // 1. Create Return Header as PENDING
        PurchaseReturn purchaseReturn = PurchaseReturn.builder()
                .tenantId(tenantId)
                .vendorId(dto.getVendorId())
                .prNumber(DocumentNumberUtil.generate(DocPrefix.PR))
                .goodsReceiptId(dto.getGoodsReceiptId())
                .warehouseId(dto.getWarehouseId())
                .returnDate(System.currentTimeMillis())
                .reason(dto.getReason())
                .prStatus(ReturnStatus.PENDING) // Initial state
                .build();

        returnRepository.save(purchaseReturn);

        // 2. Save Items
        List<PurchaseReturnItem> returnItems = dto.getItems().stream().map(itemDto ->
                PurchaseReturnItem.builder()
                        .purchaseReturnId(purchaseReturn.getId())
                        .itemId(itemDto.getItemId())
                        .returnQty(itemDto.getReturnQty())
                        .refundPrice(itemDto.getRefundPrice())
                        .batchNumber(itemDto.getBatchNumber())
                        .build()
        ).collect(Collectors.toList());

        returnItemRepository.saveAll(returnItems);

        return CommonResponse.builder()
                .id(purchaseReturn.getId().toString())
                .message("Purchase return draft created. Pending completion.")
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> updatePurchaseReturn(Long returnId, PurchaseReturnDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // 1. Fetch the existing entity with its items
        PurchaseReturn pr = returnRepository.findByIdAndTenantId(returnId, tenantId)
                .orElseThrow(() -> new CommonException("Return record not found", HttpStatus.NOT_FOUND));

        // 2. Guardrail: Only update if PENDING
        if (pr.getPrStatus() != ReturnStatus.PENDING) {
            throw new CommonException("Cannot update a completed return", HttpStatus.BAD_REQUEST);
        }

        // 3. Update Header
        pr.setVendorId(dto.getVendorId());
        pr.setWarehouseId(dto.getWarehouseId());
        pr.setReason(dto.getReason());

        // 4. Selective Item Update (The "Normal" way)
        List<PurchaseReturnItem> currentItems = pr.getPurchaseReturnItems();

        // Convert DTOs to Entities and synchronize the list
        List<PurchaseReturnItem> updatedItems = dto.getItems().stream().map(itemDto -> {
            if (itemDto.getId() != null) {
                // Find existing item to update
                return currentItems.stream()
                        .filter(i -> i.getId().equals(itemDto.getId()))
                        .findFirst()
                        .map(existing -> {
                            existing.setItemId(itemDto.getItemId());
                            existing.setReturnQty(itemDto.getReturnQty());
                            existing.setRefundPrice(itemDto.getRefundPrice());
                            existing.setBatchNumber(itemDto.getBatchNumber());
                            return existing;
                        }).orElseGet(() -> createNewItem(pr.getId(), itemDto)); // Edge case: ID provided but not found
            } else {
                // It's a brand new item added in the UI
                return createNewItem(pr.getId(), itemDto);
            }
        }).collect(Collectors.toList());

        // 5. Update the collection
        currentItems.clear();
        currentItems.addAll(updatedItems);

        returnRepository.save(pr);

        return CommonResponse.builder()
                .id(pr.getId().toString())
                .message("Return updated successfully via selective update")
                .build();
    }

    private PurchaseReturnItem createNewItem(Long prId, PurchaseReturnItemDto dto) {
        return PurchaseReturnItem.builder()
                .purchaseReturnId(prId)
                .itemId(dto.getItemId())
                .returnQty(dto.getReturnQty())
                .refundPrice(dto.getRefundPrice())
                .batchNumber(dto.getBatchNumber())
                .build();
    }


    @Override
    @Transactional
    public CommonResponse<?> updateStatus(Long returnId, ReturnStatus newStatus) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        // Fetch the Return Record
        PurchaseReturn pr = returnRepository.findByIdAndTenantId(returnId, tenantId)
                .orElseThrow(() -> new CommonException("Return record not found", HttpStatus.NOT_FOUND));

        // Prevent duplicate processing
        if (pr.getPrStatus() == ReturnStatus.COMPLETED) {
            throw new CommonException("Return is already completed and stock has been adjusted.", HttpStatus.BAD_REQUEST);
        }

        // If moving to COMPLETED, perform the stock movement
        if (newStatus == ReturnStatus.COMPLETED) {
            List<PurchaseReturnItem> items = returnItemRepository.findByPurchaseReturnId(returnId);

            for (PurchaseReturnItem item : items) {
                StockUpdateDto stockUpdate = StockUpdateDto.builder()
                        .itemId(item.getItemId())
                        .warehouseId(pr.getWarehouseId())
                        .quantity(item.getReturnQty())
                        .transactionType(MovementType.OUT)
                        .referenceType(ReferenceType.PURCHASE_RETURN)
                        .referenceId(pr.getId())
                        .batchNumber(item.getBatchNumber())
                        .build();

                stockService.updateStock(stockUpdate);
            }
        }

        pr.setPrStatus(newStatus);
        returnRepository.save(pr);

        return CommonResponse.builder()
                .id(pr.getId().toString())
                .message("Return status updated to " + newStatus)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public PurchaseReturnDto getReturnDetails(Long returnId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        PurchaseReturn pr = returnRepository.findByIdAndTenantId(returnId, tenantId)
                .orElseThrow(() -> new CommonException("Return record not found", HttpStatus.BAD_REQUEST));

        List<PurchaseReturnItem> items = returnItemRepository.findByPurchaseReturnId(returnId);

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(List.of(pr.getVendorId()));

        return mapToReturnDto(pr, items, vendorDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseReturnDto> getAllReturns(Integer page, Integer size) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PurchaseReturn> prPage = returnRepository.findAllByTenantId(tenantId, pageable);

        List<Long> returnIds = prPage.stream().map(PurchaseReturn::getId).toList();

        List<PurchaseReturnItem> allItems = returnItemRepository.findByPurchaseReturnIdIn(returnIds);

        Map<Long, List<PurchaseReturnItem>> itemsByReturnId = allItems.stream()
                .collect(Collectors.groupingBy(PurchaseReturnItem::getPurchaseReturnId));

        Set<Long> vendorIds = prPage.stream().map(PurchaseReturn::getVendorId).collect(Collectors.toSet());

        Map<Long, UserMiniDto> vendorDetails = authServiceClient.getBulkUserDetails(vendorIds.stream().toList());

        return prPage.map(pr -> mapToReturnDto(pr, itemsByReturnId.get(pr.getId()), vendorDetails));
    }

    private PurchaseReturnDto mapToReturnDto(PurchaseReturn pr, List<PurchaseReturnItem> items,
                                             Map<Long, UserMiniDto> vendorDetails) {

        List<PurchaseReturnItemDto> itemDtos = items.stream()
                .map(this::mapToItemDto)
                .toList();

        return PurchaseReturnDto.builder()
                .id(pr.getId())
                .vendorId(pr.getVendorId())
                .vendorDetails(vendorDetails.get(pr.getVendorId()))
                .goodsReceiptId(pr.getGoodsReceiptId())
                .prNumber(pr.getPrNumber())
                .reason(pr.getReason())
                .items(itemDtos)
                .status(pr.getPrStatus())
                .build();
    }

    private PurchaseReturnItemDto mapToItemDto(PurchaseReturnItem item) {
        return PurchaseReturnItemDto.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .returnQty(item.getReturnQty())
                .refundPrice(item.getRefundPrice())
                .build();
    }

}
