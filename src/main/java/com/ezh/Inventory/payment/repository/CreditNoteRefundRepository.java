package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.CreditNoteRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRefundRepository extends JpaRepository<CreditNoteRefund, Long> {

    Optional<CreditNoteRefund> findByIdAndTenantId(Long id, Long tenantId);

    /** All refunds for a specific credit note */
    List<CreditNoteRefund> findByCreditNoteIdAndTenantIdOrderByRefundDateDesc(
            Long creditNoteId, Long tenantId);

    /** Total CLEARED cash paid out for a specific CN */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM CreditNoteRefund r " +
           "WHERE r.creditNote.id = :creditNoteId AND r.tenantId = :tenantId AND r.status = 'CLEARED'")
    BigDecimal getTotalClearedRefunds(
            @Param("creditNoteId") Long creditNoteId,
            @Param("tenantId") Long tenantId);
}
