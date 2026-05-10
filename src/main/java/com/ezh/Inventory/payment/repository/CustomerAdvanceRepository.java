package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.approval.entity.ApprovalConfig;
import com.ezh.Inventory.payment.entity.enums.AdvanceStatus;
import com.ezh.Inventory.payment.entity.CustomerAdvance;
import org.hibernate.annotations.TenantId;
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
public interface CustomerAdvanceRepository extends JpaRepository<CustomerAdvance, Long> {

    Optional<CustomerAdvance> findByIdAndTenantId(Long id, Long tenantId);

    Page<CustomerAdvance> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * All advances for a customer that still have usable balance (for FIFO application)
     */
    List<CustomerAdvance> findByCustomerIdAndTenantIdAndAvailableBalanceGreaterThanOrderByReceivedDateAsc(
            Long customerId, Long tenantId, BigDecimal minBalance);

    /**
     * All advances for a customer, newest first
     */
    List<CustomerAdvance> findByCustomerIdAndTenantIdOrderByReceivedDateDesc(
            Long customerId, Long tenantId);

    /**
     * Total available advance balance for a customer
     */
    @Query("SELECT COALESCE(SUM(a.availableBalance), 0) FROM CustomerAdvance a " +
            "WHERE a.customerId = :customerId AND a.tenantId = :tenantId " +
            "AND a.status NOT IN ('CANCELLED', 'FULLY_UTILIZED', 'REFUNDED')")
    BigDecimal getTotalAvailableBalance(
            @Param("customerId") Long customerId,
            @Param("tenantId") Long tenantId);

    List<CustomerAdvance> findByCustomerIdAndTenantIdAndStatusOrderByReceivedDateDesc(
            Long customerId, Long tenantId, AdvanceStatus status);
}
