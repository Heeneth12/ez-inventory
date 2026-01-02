package com.ezh.Inventory.sales.order.repository;

import com.ezh.Inventory.sales.order.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByIdAndTenantId(Long id, Long tenantId);

    Page<SalesOrder> findByTenantId(Long tenantId, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM sales_order so
                    WHERE so.tenant_id = :tenantId
                      AND (:id IS NULL OR so.id = :id)
                      AND (:status IS NULL OR so.status = :status)
                      AND (:customerId IS NULL OR so.customer_id = :customerId)
                      AND (:warehouseId IS NULL OR so.warehouse_id = :warehouseId)
                    """,
            nativeQuery = true
    )
    List<SalesOrder> getAllSalesOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId
    );


    @Query(
            value = """
                    SELECT * FROM sales_order so
                    WHERE so.tenant_id = :tenantId
                      AND (:id IS NULL OR so.id = :id)
                      AND (CAST(:status AS text) IS NULL OR so.status = CAST(:status AS sales_order_status))
                      AND (:customerId IS NULL OR so.customer_id = :customerId)
                      AND (:warehouseId IS NULL OR so.warehouse_id = :warehouseId)
                      AND (
                            (CAST(:fromDate AS date) IS NULL OR so.order_date >= CAST(:fromDate AS date))
                            AND (CAST(:toDate AS date) IS NULL OR so.order_date <= CAST(:toDate AS date))
                          )
                      AND (
                            CAST(:searchQuery AS text) IS NULL
                            OR LOWER(so.order_number) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS text), '%'))
                            OR LOWER(so.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS text), '%'))
                          )
                    """,
            nativeQuery = true
    )
    Page<SalesOrder> getAllSalesOrders(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("status") String status,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            Pageable pageable
    );

}
