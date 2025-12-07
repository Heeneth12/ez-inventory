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
import com.ezh.Inventory.stock.repository.StockRepository;
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
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseReturnServiceImpl implements PurchaseReturnService {

    private final PurchaseReturnRepository returnRepository;
    private final PurchaseReturnItemRepository returnItemRepository;
    private final StockService stockService;
    private final StockRepository stockRepository; // Need to fetch warehouse info if not provided

    @Override
    @Transactional
    public CommonResponse createPurchaseReturn(PurchaseReturnDto dto) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        // VALIDATION: Ensure warehouse is selected so we know where to remove stock from
        if (dto.getWarehouseId() == null) {
            throw new CommonException("Warehouse ID is required for returns", HttpStatus.BAD_REQUEST);
        }

        // 1. Create Return Header
        PurchaseReturn purchaseReturn = PurchaseReturn.builder()
                .tenantId(tenantId)
                .supplierId(dto.getSupplierId())
                .goodsReceiptId(dto.getGoodsReceiptId())
                .warehouseId(dto.getWarehouseId()) // Store the warehouse ID
                .returnDate(System.currentTimeMillis())
                .reason(dto.getReason())
                .status(ReturnStatus.COMPLETED)
                .build();
        returnRepository.save(purchaseReturn);

        List<PurchaseReturnItem> returnItems = new ArrayList<>();

        // 2. Process Items
        for (PurchaseReturnItemDto itemDto : dto.getItems()) {

            // A. Save Return Item (including Batch Number for history)
            PurchaseReturnItem item = PurchaseReturnItem.builder()
                    .purchaseReturnId(purchaseReturn.getId())
                    .itemId(itemDto.getItemId())
                    .returnQty(itemDto.getReturnQty())
                    .refundPrice(itemDto.getRefundPrice())
                    .batchNumber(itemDto.getBatchNumber()) // <--- NEW: Save Batch info
                    .build();
            returnItems.add(item);

            // B. UPDATE STOCK (OUT)
            // Use the specific batch number so correct price is used and batch qty is reduced
            StockUpdateDto stockUpdate = StockUpdateDto.builder()
                    .itemId(itemDto.getItemId())
                    .warehouseId(dto.getWarehouseId()) // Use DTO value, not hardcoded
                    .quantity(itemDto.getReturnQty())
                    .transactionType(MovementType.OUT)
                    .referenceType(ReferenceType.PURCHASE_RETURN)
                    .referenceId(purchaseReturn.getId())
                    .batchNumber(itemDto.getBatchNumber()) // <--- NEW: Deduct from specific batch
                    .build();

            stockService.updateStock(stockUpdate);
        }

        returnItemRepository.saveAll(returnItems);

        return CommonResponse.builder()
                .id(purchaseReturn.getId().toString())
                .message("Return Processed successfully")
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public PurchaseReturnDto getReturnDetails(Long returnId) throws CommonException {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        PurchaseReturn pr = returnRepository.findByIdAndTenantId(returnId, tenantId)
                .orElseThrow(() -> new CommonException("Return record not found", HttpStatus.BAD_REQUEST));

        List<PurchaseReturnItem> items = returnItemRepository.findByPurchaseReturnId(returnId);

        return mapToReturnDto(pr, items);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseReturnDto> getAllReturns(Integer page, Integer size) {

        Long tenantId = UserContextUtil.getTenantIdOrThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PurchaseReturn> prPage = returnRepository.findAllByTenantId(tenantId, pageable);

        return prPage.map(pr -> mapToReturnDto(pr, Collections.emptyList()));
    }

    private PurchaseReturnDto mapToReturnDto(PurchaseReturn pr, List<PurchaseReturnItem> items) {

        List<PurchaseReturnItemDto> itemDtos = items.stream()
                .map(this::mapToItemDto)
                .toList();

        return PurchaseReturnDto.builder()
                .supplierId(pr.getSupplierId())
                .goodsReceiptId(pr.getGoodsReceiptId())
                .reason(pr.getReason())
                .items(itemDtos)
                .build();
    }

    private PurchaseReturnItemDto mapToItemDto(PurchaseReturnItem item) {
        return PurchaseReturnItemDto.builder()
                .itemId(item.getItemId())
                .returnQty(item.getReturnQty())
                .refundPrice(item.getRefundPrice())
                .build();
    }

}
