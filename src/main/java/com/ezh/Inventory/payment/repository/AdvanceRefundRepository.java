package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.AdvanceRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvanceRefundRepository extends JpaRepository<AdvanceRefund, Long> {

    Optional<AdvanceRefund> findByIdAndTenantId(Long id, Long tenantId);

    /** All refunds from a specific advance */
    List<AdvanceRefund> findByAdvanceIdAndTenantIdOrderByRefundDateDesc(
            Long advanceId, Long tenantId);

    /** Total CLEARED refund amount drawn from a specific advance */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM AdvanceRefund r " +
           "WHERE r.advance.id = :advanceId AND r.tenantId = :tenantId AND r.status = 'CLEARED'")
    BigDecimal getTotalClearedRefunds(
            @Param("advanceId") Long advanceId,
            @Param("tenantId") Long tenantId);
}
