package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.AdvanceUtilization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdvanceUtilizationRepository extends JpaRepository<AdvanceUtilization, Long> {

    /** All utilizations for a specific advance record */
    List<AdvanceUtilization> findByAdvanceIdAndTenantIdOrderByUtilizationDateDesc(
            Long advanceId, Long tenantId);

    /** All advance utilizations applied to a specific invoice */
    List<AdvanceUtilization> findByInvoiceIdAndTenantIdOrderByUtilizationDateDesc(
            Long invoiceId, Long tenantId);

    /** Total CONFIRMED advance amount applied to an invoice (for invoice payment summary) */
    @Query("SELECT COALESCE(SUM(u.utilizedAmount), 0) FROM AdvanceUtilization u " +
           "WHERE u.invoiceId = :invoiceId AND u.tenantId = :tenantId AND u.status = 'CONFIRMED'")
    BigDecimal getTotalConfirmedForInvoice(
            @Param("invoiceId") Long invoiceId,
            @Param("tenantId") Long tenantId);
}
