package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.CreditNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Optional<CreditNote> findByIdAndTenantId(Long id, Long tenantId);

    Page<CreditNote> findByTenantId(Long tenantId, Pageable pageable);

    Optional<CreditNote> findBySourceReturnIdAndTenantId(Long sourceReturnId, Long tenantId);

    /** All credit notes for a customer with available balance (for FIFO application) */
    List<CreditNote> findByCustomerIdAndTenantIdAndAvailableBalanceGreaterThanOrderByIssueDateAsc(
            Long customerId, Long tenantId, BigDecimal minBalance);

    /** All credit notes for a customer, newest first */
    List<CreditNote> findByCustomerIdAndTenantIdOrderByIssueDateDesc(
            Long customerId, Long tenantId);

    /** Total available credit note balance for a customer */
    @Query("SELECT COALESCE(SUM(c.availableBalance), 0) FROM CreditNote c " +
           "WHERE c.customerId = :customerId AND c.tenantId = :tenantId " +
           "AND c.status NOT IN ('CANCELLED', 'FULLY_UTILIZED', 'REFUNDED')")
    BigDecimal getTotalAvailableBalance(
            @Param("customerId") Long customerId,
            @Param("tenantId") Long tenantId);
}
