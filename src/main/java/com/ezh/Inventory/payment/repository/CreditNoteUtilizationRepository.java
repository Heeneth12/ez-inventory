package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.CreditNoteUtilization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CreditNoteUtilizationRepository extends JpaRepository<CreditNoteUtilization, Long> {

    /** All utilizations for a specific credit note */
    List<CreditNoteUtilization> findByCreditNoteIdAndTenantIdOrderByUtilizationDateDesc(
            Long creditNoteId, Long tenantId);

    /** All CN utilizations applied to a specific invoice */
    List<CreditNoteUtilization> findByInvoiceIdAndTenantIdOrderByUtilizationDateDesc(
            Long invoiceId, Long tenantId);

    /** Total CONFIRMED CN amount applied to an invoice */
    @Query("SELECT COALESCE(SUM(u.utilizedAmount), 0) FROM CreditNoteUtilization u " +
           "WHERE u.invoiceId = :invoiceId AND u.tenantId = :tenantId AND u.status = 'CONFIRMED'")
    BigDecimal getTotalConfirmedForInvoice(
            @Param("invoiceId") Long invoiceId,
            @Param("tenantId") Long tenantId);
}
