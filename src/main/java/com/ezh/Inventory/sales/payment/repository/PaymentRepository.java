package com.ezh.Inventory.sales.payment.repository;

import com.ezh.Inventory.sales.payment.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByTenantId(Long tenantId, Pageable pageable);

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT SUM(p.unallocatedAmount) FROM Payment p WHERE p.customer.id = :customerId AND p.tenantId = :tenantId")
    BigDecimal getTotalUnallocatedByCustomer(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    List<Payment> findByCustomerIdAndUnallocatedAmountGreaterThan(Long customerId, BigDecimal minAmount);

    @Query("SELECT COALESCE(SUM(p.unallocatedAmount), 0) FROM Payment p " +
            "WHERE p.customer.id = :customerId AND " +
            "p.tenantId = :tenantId AND p.unallocatedAmount > 0")
    BigDecimal findTotalAvailableCredit(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
}
