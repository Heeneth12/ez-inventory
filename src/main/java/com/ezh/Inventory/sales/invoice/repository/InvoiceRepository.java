package com.ezh.Inventory.sales.invoice.repository;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoicePaymentStatus;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
              AND (:invoiceNumber IS NULL OR i.invoiceNumber = :invoiceNumber)
              AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
              AND (:statuses IS NULL OR i.status IN :statuses)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
            """)
    List<Invoice> searchInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("salesOrderId") Long salesOrderId,
            @Param("statuses") List<InvoiceStatus> statuses,
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

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.tenantId = :tenantId
              AND (:id IS NULL OR i.id = :id)
              AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
              AND (:statuses IS NULL OR i.status IN :statuses)
              AND (:paymentStatuses IS NULL OR i.paymentStatus IN :paymentStatuses)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR i.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR i.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(i.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    Page<Invoice> getAllInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("salesOrderId") Long salesOrderId,
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("paymentStatuses") List<InvoicePaymentStatus> paymentStatuses,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.tenantId = :tenantId
              AND (:id IS NULL OR i.id = :id)
              AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
              AND (:statuses IS NULL OR i.status IN :statuses)
              AND (:paymentStatuses IS NULL OR i.paymentStatus IN :paymentStatuses)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
              AND (
                    (CAST(:fromDate AS timestamp) IS NULL OR i.createdAt >= :fromDate)
                    AND (CAST(:toDate AS timestamp) IS NULL OR i.createdAt <= :toDate)
                  )
              AND (
                    CAST(:searchQuery AS string) IS NULL
                    OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                    OR LOWER(i.remarks) LIKE LOWER(CONCAT('%', CAST(:searchQuery AS string), '%'))
                  )
            """)
    List<Invoice> getAllInvoices(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("salesOrderId") Long salesOrderId,
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("paymentStatuses") List<InvoicePaymentStatus> paymentStatuses,
            @Param("customerId") Long customerId,
            @Param("warehouseId") Long warehouseId,
            @Param("searchQuery") String searchQuery,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

}
