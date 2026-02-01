package com.ezh.Inventory.stock.repository;

import com.ezh.Inventory.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByItemIdAndWarehouseIdAndTenantId(Long itemId, Long warehouseId, Long tenantId);

    Optional<Stock> findByItemId(Long itemId);

    Page<Stock> findByTenantId(Long tenantId, Pageable pageable);

    // 1. Total Stock Value
    @Query("SELECT COALESCE(SUM(s.stockValue), 0) FROM Stock s WHERE s.tenantId = :tenantId AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId)")
    BigDecimal getTotalStockValue(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);

    // 2. Net Stock Movement (Returns a projection or Object array with Total IN and Total OUT)
    @Query("SELECT COALESCE(SUM(s.inQty), 0) as totalIn, COALESCE(SUM(s.outQty), 0) as totalOut FROM Stock s WHERE s.tenantId = :tenantId AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId)")
    Map<String, Object> getStockMovementSummary(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);
    // Note: If using Map<String, Object> is tricky with your JPA version, use a Projection Interface.

    // 3. Out of Stock Count
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.tenantId = :tenantId AND s.closingQty <= 0 AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId)")
    long countOutOfStockItems(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);

    // 4. Fast Moving Items (High Out Qty)
    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId) ORDER BY s.outQty DESC")
    List<Stock> findFastMovingItems(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId, Pageable pageable);
}
