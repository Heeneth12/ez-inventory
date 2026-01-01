package com.ezh.Inventory.stock.service;

import com.ezh.Inventory.items.repository.ItemRepository;
import com.ezh.Inventory.stock.dto.*;
import com.ezh.Inventory.stock.entity.MovementType;
import com.ezh.Inventory.stock.entity.Stock;
import com.ezh.Inventory.stock.entity.StockBatch;
import com.ezh.Inventory.stock.entity.StockLedger;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockLedgerRepository stockLedgerRepository;
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
        String consumedBatches = dto.getBatchNumber();
        String finalBatchUsed = dto.getBatchNumber();

        if (dto.getTransactionType() == MovementType.OUT) {

            // Check Global Availability first
            if (beforeQty < qty) {
                throw new BadRequestException("Not enough stock available globally. Current: " + beforeQty);
            }

            boolean isSpecificBatch = dto.getBatchNumber() != null && !dto.getBatchNumber().isEmpty();

            // --- BATCH LOGIC START ---
            if (isSpecificBatch) {
                // CASE 1: User wants a specific batch (e.g., "BATCH-001")
                StockBatch batch = stockBatchRepository
                        .findByItemIdAndBatchNumberAndWarehouseId(dto.getItemId(), dto.getBatchNumber(), dto.getWarehouseId())
                        .orElseThrow(() -> new BadRequestException("Batch " + dto.getBatchNumber() + " not found"));

                if (batch.getRemainingQty() < qty) {
                    throw new BadRequestException("Not enough stock in Batch " + dto.getBatchNumber());
                }

                batch.setRemainingQty(batch.getRemainingQty() - qty);
                stockBatchRepository.save(batch);

                // Use specific batch price for ledger
                costForLedger = batch.getBuyPrice();
                finalBatchUsed = batch.getBatchNumber();

            } else {
                // CASE 2: No batch provided (null). AUTO-PICK (FIFO)
                // This stops the code from searching for a batch named "null"
                String allocatedBatchStr = performFifoDeduction(dto.getItemId(), dto.getWarehouseId(), qty);

                // IMPORTANT: Update the DTO batch number so it gets returned in the response
                // This ensures the InvoiceService knows which batches were actually picked
                dto.setBatchNumber(allocatedBatchStr);
                finalBatchUsed = allocatedBatchStr;
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
                // here we ddidt sent any data to take in invoice service
                .status(Status.SUCCESS)
                .data(finalBatchUsed)
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
    public List<ItemStockSearchDto> searchItemsWithBatches(String query, Long warehouseId) {

        // 1. Fetch Flat Data
        List<StockSearchProjection> rawData = stockBatchRepository.searchStockWithBatches(warehouseId, query);

        // 2. Group by Item ID
        Map<Long, List<StockSearchProjection>> groupedData = rawData.stream()
                .collect(Collectors.groupingBy(StockSearchProjection::getItemId));

        // 3. Transform into Nested DTOs
        return groupedData.values().stream().map(batchList -> {
            // Since grouped by ID, Item info is same for all rows in 'batchList'
            StockSearchProjection first = batchList.getFirst();

            // Map the list of batches
            List<BatchDetailDto> batchDtos = batchList.stream()
                    .map(b -> BatchDetailDto.builder()
                            .batchNumber(b.getBatchNumber())
                            .buyPrice(b.getBuyPrice())
                            .remainingQty(b.getRemainingQty())
                            .expiryDate(b.getExpiryDate())
                            .build())
                    .collect(Collectors.toList());
            // Map the Parent Item
            return ItemStockSearchDto.builder()
                    .itemId(first.getItemId())
                    .name(first.getItemName())
                    .code(first.getItemCode())
                    .sku(first.getItemSku())
                    .batches(batchDtos) // Attach the list
                    .build();
        }).collect(Collectors.toList());
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

    @Override
    @Transactional(readOnly = true)
    public StockDashboardDto getStockDashboard(Long warehouseId) throws CommonException {
        Long tenantId = getTenantIdOrThrow();

        // 1. Total Stock Value
        BigDecimal totalValue = stockRepository.getTotalStockValue(tenantId, warehouseId);

        // 2. Net Movement (Using the Map approach for simplicity, or use a Projection)
        // If your repo returns Map<String, Object>, it usually returns List<Map<...>> or Object[].
        // Let's assume we fetch raw Object[] for safety:
        // @Query("SELECT sum(s.inQty), sum(s.outQty) ...") -> returns Object[]
        // For this example, let's do it cleanly via the repository logic below:

        // Note: You might need to adjust the Repository method to return an Interface Projection for type safety.
        // Here is a manual implementation assuming distinct repo calls or a projection wrapper:
        Integer totalIn = 0;
        Integer totalOut = 0;

        // Simulating the repo call for movement (Ideally use a Projection interface)
        Map<String, Object> movement = stockRepository.getStockMovementSummary(tenantId, warehouseId);
        // JPA Maps usually return "0": value.
        if (movement != null && !movement.isEmpty()) {
            // Depending on driver, might need casting
            // totalIn = ((Number) movement.get("totalIn")).intValue();
            // totalOut = ((Number) movement.get("totalOut")).intValue();

            // ALTERNATIVE: Simpler Repository Calls if Map is annoying:
            // Integer getSumInQty(...); Integer getSumOutQty(...);
        }

        // Let's assume we split the repo calls for cleaner Java code:
        // Integer totalIn = stockRepository.sumInQty(tenantId, warehouseId);
        // Integer totalOut = stockRepository.sumOutQty(tenantId, warehouseId);

        // 3. Out of Stock Items
        long outOfStockCount = stockRepository.countOutOfStockItems(tenantId, warehouseId);

        // 4. Fast-Moving Items (Top 5)
        Pageable topFive = PageRequest.of(0, 5);
        List<Stock> fastMovingStocks = stockRepository.findFastMovingItems(tenantId, warehouseId, topFive);

        List<StockDto> fastMovingDtos = fastMovingStocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Construct Response
        return StockDashboardDto.builder()
                .totalStockValue(totalValue)
                .totalItemsOutOfStock(outOfStockCount)
                // Assuming we fetched these or they came from the map
                .totalInQty(totalIn)
                .totalOutQty(totalOut)
                .netMovementQty(totalIn - totalOut)
                .fastMovingItems(fastMovingDtos)
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
                .averageCost(stock.getAverageCost())
                .stockValue(stock.getStockValue())
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


    private String performFifoDeduction(Long itemId, Long warehouseId, int qtyRequired) {
        // 1. Fetch batches with stock, ordered by Creation Date (Oldest First)
        List<StockBatch> availableBatches = stockBatchRepository
                .findByItemIdAndWarehouseIdAndRemainingQtyGreaterThanOrderByCreatedAtAsc(itemId, warehouseId, 0);

        if (availableBatches.isEmpty()) {
            throw new BadRequestException("No batches available with stock for this item");
        }

        int qtyToDeduct = qtyRequired;
        List<String> usedBatchNumbers = new ArrayList<>();

        for (StockBatch batch : availableBatches) {
            if (qtyToDeduct <= 0) break;

            int available = batch.getRemainingQty();
            int take = Math.min(available, qtyToDeduct);

            // Deduct
            batch.setRemainingQty(available - take);
            stockBatchRepository.save(batch);

            usedBatchNumbers.add(batch.getBatchNumber());
            qtyToDeduct -= take;
        }

        if (qtyToDeduct > 0) {
            throw new BadRequestException("Data Inconsistency: Global stock says available, but Batches are empty.");
        }

        return String.join(",", usedBatchNumbers);
    }
}