package com.ezh.Inventory.sales.invoice.repository;

import com.ezh.Inventory.sales.invoice.entity.Invoice;
import com.ezh.Inventory.sales.invoice.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

    @Query("""
                SELECT i FROM Invoice i
                WHERE i.tenantId = :tenantId
                  AND (:id IS NULL OR i.id = :id)
                  AND (:salesOrderId IS NULL OR i.salesOrder.id = :salesOrderId)
                  AND (:status IS NULL OR i.status = :status)
                  AND (:customerId IS NULL OR i.customer.id = :customerId)
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
}
