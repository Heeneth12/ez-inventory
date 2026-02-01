package com.ezh.Inventory.stock.repository;

import com.ezh.Inventory.stock.dto.StockSearchProjection;
import com.ezh.Inventory.stock.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // Find specific batch to sell from
    Optional<StockBatch> findByItemIdAndBatchNumberAndWarehouseId(Long itemId, String batchNumber, Long warehouseId);

    List<StockBatch> findByItemIdAndWarehouseIdAndRemainingQtyGreaterThanOrderByCreatedAtAsc(Long itemId, Long warehouseId, Integer minQty);

    // Find all available batches for an item (Useful for FIFO)
    @Query("SELECT b FROM StockBatch b WHERE b.itemId = :itemId AND b.warehouseId = :warehouseId AND b.remainingQty > 0 ORDER BY b.createdAt ASC")
    List<StockBatch> findAvailableBatches(Long itemId, Long warehouseId);

    @Query("SELECT sb.itemId as itemId, " +
            "i.name as itemName, " +
            "i.itemCode as itemCode, " +
            "i.sku as itemSku, " +
            "sb.batchNumber as batchNumber, " +
            "sb.buyPrice as buyPrice, " +
            "sb.remainingQty as remainingQty, " +
            "sb.expiryDate as expiryDate " +
            "FROM StockBatch sb " +
            "JOIN Item i ON sb.itemId = i.id " +
            "WHERE sb.tenantId = :tenantId " +
            "AND sb.warehouseId = :warehouseId " +
            "AND sb.remainingQty > 0 " +
            "AND (:itemId IS NULL OR sb.itemId = :itemId) " +
            "AND (:query IS NULL OR (LOWER(i.name) LIKE :query OR LOWER(i.itemCode) LIKE :query)) " +
            "ORDER BY i.name ASC, sb.expiryDate ASC")
    List<StockSearchProjection> searchStockWithBatches(
            @Param("tenantId") Long tenantId,
            @Param("warehouseId") Long warehouseId,
            @Param("itemId") Long itemId,
            @Param("query") String query);
}