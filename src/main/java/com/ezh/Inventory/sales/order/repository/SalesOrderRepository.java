package com.ezh.Inventory.sales.order.repository;

import com.ezh.Inventory.sales.order.dto.SalesConversionCountProjection;
import com.ezh.Inventory.sales.order.dto.SalesOrderStats;
import com.ezh.Inventory.sales.order.entity.SalesOrder;
import com.ezh.Inventory.sales.order.entity.SalesOrderSource;
import com.ezh.Inventory.sales.order.entity.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByIdAndTenantId(Long id, Long tenantId);

    Optional<SalesOrder> findByOrderNumberAndTenantId(String OrderNumber, Long tenantId);

    Page<SalesOrder> findByTenantId(Long tenantId, Pageable pageable);

    @Query("""
            SELECT so FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (:id IS NULL OR so.id = :id)
              AND (:soNumber IS NULL OR so.orderNumber = :soNumber)
              AND (:statuses IS NULL OR so.status IN :statuses)
              AND (:customerId IS NULL OR so.customerId = :customerId)
              AND (:warehouseId IS NULL OR so.warehouseId = :warehouseId)
            """)
    List<SalesOrder> getAllSalesOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("soNumber") String soNumber,
            @Param("statuses") List<SalesOrderStatus> statuses,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId
    );


    @Query("""
            SELECT so FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (:id IS NULL OR so.id = :id)
              AND (:statuses IS NULL OR so.status IN :statuses)
              AND (:sources IS NULL OR so.source IN :sources)
              AND (:customerId IS NULL OR so.customerId = :customerId)
              AND (:warehouseId IS NULL OR so.warehouseId = :warehouseId)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR so.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR so.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(so.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<SalesOrder> getAllSalesOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("statuses") List<SalesOrderStatus> statuses,
            @Param("sources") List<SalesOrderSource> sources,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
            SELECT so FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (:id IS NULL OR so.id = :id)
              AND (:statuses IS NULL OR so.status IN :statuses)
              AND (:sources IS NULL OR so.source IN :sources)
              AND (:customerId IS NULL OR so.customerId = :customerId)
              AND (:warehouseId IS NULL OR so.warehouseId = :warehouseId)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR so.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR so.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(so.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    List<SalesOrder> getAllSalesOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("statuses") List<SalesOrderStatus> statuses,
            @Param("sources") List<SalesOrderSource> sources,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
            SELECT
                COUNT(so) AS totalSalesOrders,
                COUNT(CASE WHEN so.status IN ('PARTIALLY_INVOICED', 'FULLY_INVOICED') THEN 1 END) AS convertedToInvoice
            FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (:customerId IS NULL OR so.customerId = :customerId)
              AND (:warehouseId IS NULL OR so.warehouseId = :warehouseId)
              AND (CAST(:fromDate AS timestamp) IS NULL OR so.createdAt >= :fromDate)
              AND (CAST(:toDate AS timestamp) IS NULL OR so.createdAt <= :toDate)
            """)
    SalesConversionCountProjection countSalesOrderConversion(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
            SELECT
                FUNCTION('DATE', so.createdAt) AS reportDate,
                COUNT(so) AS totalSalesOrders,
                COUNT(CASE WHEN so.status IN ('PARTIALLY_INVOICED', 'FULLY_INVOICED') THEN 1 END) AS convertedToInvoice,
                SUM(so.grandTotal) AS totalSalesValue,
                SUM(CASE WHEN so.status IN ('PARTIALLY_INVOICED', 'FULLY_INVOICED') THEN so.grandTotal ELSE 0 END) AS convertedSalesValue,
                COUNT(CASE WHEN so.status = 'PENDING_APPROVAL' THEN 1 END) AS pendingApprovalCount,
                COUNT(CASE WHEN so.status IN ('CANCELLED', 'REJECTED') THEN 1 END) AS cancelledRejectedCount
            FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (:customerId IS NULL OR so.customerId = :customerId)
              AND (:warehouseId IS NULL OR so.warehouseId = :warehouseId)
              AND (CAST(:fromDate AS timestamp) IS NULL OR so.createdAt >= :fromDate)
              AND (CAST(:toDate AS timestamp) IS NULL OR so.createdAt <= :toDate)
            GROUP BY FUNCTION('DATE', so.createdAt)
            ORDER BY FUNCTION('DATE', so.createdAt)
            """)
    List<SalesConversionDateProjection> getSalesOrderConversionReport(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    @Query("""
            SELECT 
                COALESCE(SUM(so.grandTotal), 0) AS totalValue,
                COALESCE(SUM(CASE WHEN so.status IN ('PARTIALLY_INVOICED', 'FULLY_INVOICED') THEN so.grandTotal ELSE 0 END), 0) AS convertedSalesValue,
                COUNT(so) AS totalSalesOrders,
                COUNT(CASE WHEN so.status IN ('PARTIALLY_INVOICED', 'FULLY_INVOICED') THEN 1 END) AS convertedToInvoiceCount,
                COUNT(CASE WHEN so.status = 'CONFIRMED' THEN 1 END) AS confirmedCount,
                COUNT(CASE WHEN so.status = 'PENDING_APPROVAL' THEN 1 END) AS pendingApprovalCount,
                COUNT(CASE WHEN so.status IN ('CANCELLED', 'REJECTED') THEN 1 END) AS cancelledCount
            FROM SalesOrder so
            WHERE so.tenantId = :tenantId
              AND (CAST(:fromDate AS timestamp) IS NULL OR so.createdAt >= :fromDate)
              AND (CAST(:toDate AS timestamp) IS NULL OR so.createdAt <= :toDate)
            """)
    SalesOrderStats getDashboardStats(
            @Param("tenantId") Long tenantId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

}
