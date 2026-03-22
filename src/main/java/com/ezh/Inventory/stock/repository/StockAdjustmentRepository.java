package com.ezh.Inventory.stock.repository;

import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import com.ezh.Inventory.stock.entity.AdjustmentStatus;
import com.ezh.Inventory.stock.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    Page<StockAdjustment> findAllByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT sa FROM StockAdjustment sa
            WHERE sa.tenantId = :tenantId
              AND (:id IS NULL OR sa.id = :id)
              AND (:stockAdjustmentNumber IS NULL OR sa.adjustmentNumber = :stockAdjustmentNumber)
              AND (:statuses IS NULL OR sa.adjustmentStatus IN :statuses)
              AND (:warehouseId IS NULL OR sa.warehouseId = :warehouseId)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR sa.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR sa.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(sa.adjustmentNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(sa.reference) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<StockAdjustment> findAllStockAdjustment(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("stockAdjustmentNumber") String stockAdjustmentNumber,
            @Param("statuses") List<AdjustmentStatus> statuses,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
