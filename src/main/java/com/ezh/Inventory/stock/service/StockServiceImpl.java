package com.ezh.Inventory.stock.service;

import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.stock.dto.*;
import com.ezh.Inventory.stock.entity.*;
import com.ezh.Inventory.stock.repository.StockAdjustmentRepository;
import com.ezh.Inventory.stock.repository.StockBatchRepository;
import com.ezh.Inventory.stock.repository.StockLedgerRepository;
import com.ezh.Inventory.stock.repository.StockRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
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
import java.math.RoundingMode;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public CommonResponse updateStock(StockUpdateDto dto) throws CommonException {

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BadRequestException("Invalid quantity");
        }

        // 1. Fetch Stock with LOCK
        Stock stock = stockRepository
                .findByItemIdAndWarehouseIdAndTenantId(dto.getItemId(), dto.getWarehouseId(), getTenantIdOrThrow())
                .orElse(createNewStock(dto.getItemId(), dto.getWarehouseId()));

        int qty = dto.getQuantity();
        int beforeQty = stock.getClosingQty();
        BigDecimal transactionPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO;

        // Safe fetch of Average Cost
        BigDecimal currentAvgCost = stock.getAverageCost() != null ? stock.getAverageCost() : BigDecimal.ZERO;

        // --- 2. Handle IN (Purchase, Returns) ---
        if (dto.getTransactionType() == MovementType.IN) {
            // Note: Batch Creation happens in GoodsReceiptService, here we just update the Master Stock

            if (transactionPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentTotalValue = currentAvgCost.multiply(BigDecimal.valueOf(beforeQty));
                BigDecimal incomingTotalValue = transactionPrice.multiply(BigDecimal.valueOf(qty));

                BigDecimal newTotalValue = currentTotalValue.add(incomingTotalValue);
                BigDecimal newTotalQty = BigDecimal.valueOf(beforeQty + qty);

                if (newTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    stock.setAverageCost(newTotalValue.divide(newTotalQty, 2, RoundingMode.HALF_UP));
                }
            }
            stock.setInQty(stock.getInQty() + qty);
            stock.setClosingQty(beforeQty + qty);
            stock.setStockValue(stock.getAverageCost().multiply(BigDecimal.valueOf(stock.getClosingQty())));
        }

        // --- 3. Handle OUT (Sales, Adjustments) ---
        BigDecimal costForLedger = currentAvgCost; // Default to WAC

        if (dto.getTransactionType() == MovementType.OUT) {

            // Check Global Availability first
            if (beforeQty < qty) {
                throw new BadRequestException("Not enough stock available globally. Current: " + beforeQty);
            }

            // --- BATCH LOGIC START ---
            if (dto.getBatchNumber() != null && !dto.getBatchNumber().isEmpty()) {
                // User wants to sell from a SPECIFIC BATCH (e.g., The Cheap One)
                StockBatch batch = stockBatchRepository
                        .findByItemIdAndBatchNumberAndWarehouseId(dto.getItemId(), dto.getBatchNumber(), dto.getWarehouseId())
                        .orElseThrow(() -> new CommonException("Batch " + dto.getBatchNumber() + " not found", HttpStatus.BAD_REQUEST));

                if (batch.getRemainingQty() < qty) {
                    throw new BadRequestException("Not enough stock in Batch " + dto.getBatchNumber());
                }

                // Update Batch
                batch.setRemainingQty(batch.getRemainingQty() - qty);
                stockBatchRepository.save(batch);

                // KEY CHANGE: Use Batch Price, NOT Average Cost
                costForLedger = batch.getBuyPrice();
            }
            // --- BATCH LOGIC END ---

            stock.setOutQty(stock.getOutQty() + qty);
            stock.setClosingQty(beforeQty - qty);
            // Stock Value reduces based on Average, but Ledger records Specific Cost
            stock.setStockValue(stock.getAverageCost().multiply(BigDecimal.valueOf(stock.getClosingQty())));
        }

        stockRepository.save(stock);

        // 4. Ledger Entry
        StockLedger ledger = StockLedger.builder()
                .itemId(dto.getItemId())
                .tenantId(getTenantIdOrThrow())
                .warehouseId(dto.getWarehouseId())
                .transactionType(dto.getTransactionType())
                .referenceType(dto.getReferenceType())
                .referenceId(dto.getReferenceId())
                .quantity(qty)
                .beforeQty(beforeQty)
                .afterQty(stock.getClosingQty())
                .unitPrice(costForLedger)
                .build();

        stockLedgerRepository.save(ledger);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .id(String.valueOf(ledger.getId()))
                .message("Stock updated successfully")
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<StockDto> getCurrentStock(StockFilterDto filterDto, Integer page, Integer size) throws CommonException {
        Pageable pageable = PageRequest.of(page, size);
        Page<Stock> stocks = stockRepository.findAll(pageable);
        return stocks.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLedgerDto> getStockTransactions(StockFilterDto filterDto, Integer page, Integer size) throws CommonException {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockLedger> stockLedger = stockLedgerRepository.findAll(pageable);
        return stockLedger.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public CommonResponse createStockAdjustment(StockAdjustmentBatchDto batchDto) throws CommonException {
        Long tenantId = getTenantIdOrThrow();
        Long warehouseId = batchDto.getWarehouseId();
        AdjustmentMode batchMode = batchDto.getMode();

        for (StockAdjustmentItemDto itemDto : batchDto.getItems()) {

            // 1. Get Stock
            Stock stock = stockRepository
                    .findByItemIdAndWarehouseIdAndTenantId(itemDto.getItemId(), warehouseId, tenantId)
                    .orElse(createNewStock(itemDto.getItemId(), warehouseId));

            int systemQty = stock.getClosingQty();
            int finalCountedQty;
            int difference;

            // 2. Logic based on Mode
            switch (batchMode) {
                case REMOVE:
                    difference = -itemDto.getQuantity();
                    finalCountedQty = systemQty - itemDto.getQuantity();
                    break;
                case ADD:
                    difference = itemDto.getQuantity();
                    finalCountedQty = systemQty + itemDto.getQuantity();
                    break;
                case ABSOLUTE:
                default:
                    finalCountedQty = itemDto.getQuantity();
                    difference = finalCountedQty - systemQty;
                    break;
            }

            if (finalCountedQty < 0) {
                throw new BadRequestException("Adjustment would result in negative stock for Item ID: " + itemDto.getItemId());
            }

            if (difference == 0) continue;

            // 3. Save Adjustment
            StockAdjustment adjustment = StockAdjustment.builder()
                    .itemId(itemDto.getItemId())
                    .tenantId(tenantId)
                    .warehouseId(warehouseId)
                    .reasonType(itemDto.getAdjustmentType())
                    .notes(batchDto.getNotes())
                    .systemQty(systemQty)
                    .countedQty(finalCountedQty)
                    .differenceQty(difference)
                    .adjustedBy(1L)
                    .adjustedAt(System.currentTimeMillis())
                    .build();

            stockAdjustmentRepository.save(adjustment);

            // 4. Update Stock
            MovementType movementType = (difference > 0) ? MovementType.IN : MovementType.OUT;

            StockUpdateDto updateDto = StockUpdateDto.builder()
                    .itemId(itemDto.getItemId())
                    .warehouseId(warehouseId)
                    .quantity(Math.abs(difference))
                    .transactionType(movementType)
                    .referenceType(ReferenceType.ADJUSTMENT)
                    .referenceId(adjustment.getId())
                    .unitPrice(stock.getAverageCost())
                    .batchNumber(itemDto.getBatchNumber())
                    .build();

            updateStock(updateDto);
        }
        return CommonResponse.builder().message("Batch processed").build();
    }

    private Stock createNewStock(Long itemId, Long warehouseId) {
        return Stock.builder()
                .itemId(itemId)
                .tenantId(getTenantIdOrThrow())
                .warehouseId(warehouseId)
                .averageCost(BigDecimal.ZERO)
                .stockValue(BigDecimal.ZERO)
                .openingQty(0)
                .inQty(0)
                .outQty(0)
                .closingQty(0)
                .build();
    }

    private StockDto convertToDTO(Stock stock) {

        String itemName = itemRepository.findNameById(stock.getItemId())
                .orElseThrow(() -> new CommonException("item not found", HttpStatus.NOT_FOUND));

        return StockDto.builder()
                .id(stock.getId())
                .itemId(stock.getItemId())
                .itemName(itemName)
                .warehouseId(stock.getWarehouseId())
                .openingQty(stock.getOpeningQty())
                .inQty(stock.getInQty())
                .outQty(stock.getOutQty())
                .closingQty(stock.getClosingQty())
                .build();
    }

    private StockLedgerDto convertToDTO(StockLedger stockLedger) {

        String itemName = itemRepository.findNameById(stockLedger.getItemId())
                .orElseThrow(() -> new CommonException("item not found", HttpStatus.NOT_FOUND));

        return StockLedgerDto.builder()
                .id(stockLedger.getId())
                .itemId(stockLedger.getItemId())
                .itemName(itemName)
                .warehouseId(stockLedger.getWarehouseId())
                .transactionType(stockLedger.getTransactionType())
                .quantity(stockLedger.getQuantity())
                .referenceType(stockLedger.getReferenceType())
                .referenceId(stockLedger.getReferenceId())
                .beforeQty(stockLedger.getBeforeQty())
                .afterQty(stockLedger.getAfterQty())
                .build();
    }
}