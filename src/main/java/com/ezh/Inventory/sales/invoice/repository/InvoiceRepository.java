package com.ezh.Inventory.sales.invoice.repository;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT SUM(i.balance) FROM Invoice i WHERE i.customerId = :customerId AND i.tenantId = :tenantId")
    BigDecimal getTotalBalanceByCustomer(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    @Query("""
                SELECT i FROM Invoice i
                WHERE i.tenantId = :tenantId
                  AND (:id IS NULL OR i.id = :id)
                  AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
                  AND (:status IS NULL OR i.status = :status)
                  AND (:customerId IS NULL OR i.customerId = :customerId)
                  AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
            """)
    List<Invoice> searchInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("salesOrderId") Long salesOrderId,
            @Param("status") InvoiceStatus status,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId
    );

    @Query("""
                SELECT i FROM Invoice i
                WHERE i.tenantId = :tenantId
                  AND (:id IS NULL OR i.id = :id)
                  AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
                  AND (:status IS NULL OR i.status = :status)
                  AND (:customerId IS NULL OR i.customerId = :customerId)
                  AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
            """)
    Page<Invoice> getAllInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("salesOrderId") Long salesOrderId,
            @Param("status") InvoiceStatus status,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT * FROM inventory.invoice i
                    WHERE i.tenant_id = :tenantId
                      AND (:id IS NULL OR i.id = :id)
                      AND (:salesOrderId IS NULL OR i.sales_order_id = :salesOrderId)
                      AND (CAST(:status AS text) IS NULL OR i.status = CAST(:status AS text))
                      AND (:customerId IS NULL OR i.customer_id = :customerId)
                      AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
                      AND (CAST(:fromDate AS date) IS NULL OR i.invoice_date >= CAST(:fromDate AS date))
                      AND (CAST(:toDate AS date) IS NULL OR i.invoice_date <= CAST(:toDate AS date))
                      AND (
                            CAST(:searchQuery AS text) IS NULL
                            OR LOWER(i.invoice_number) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS text), '%'))
                            OR LOWER(i.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS text), '%'))
                          )
                    """,
            nativeQuery = true
    )
    Page<Invoice> getAllInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("salesOrderId") Long salesOrderId,
            @Param("status") InvoiceStatus status,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            Pageable pageable
    );
}
