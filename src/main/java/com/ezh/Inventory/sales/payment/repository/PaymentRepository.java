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

    List<Payment> findByCustomerIdAndTenantIdAndUnallocatedAmountGreaterThanOrderByPaymentDateAsc(
            Long customerId, Long tenantId, BigDecimal minAmount);

    @Query("SELECT SUM(p.unallocatedAmount) FROM Payment p WHERE p.customer.id = :customerId AND p.tenantId = :tenantId")
    BigDecimal getTotalUnallocatedByCustomer(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    List<Payment> findByCustomerIdAndUnallocatedAmountGreaterThan(Long customerId, BigDecimal minAmount);

    @Query("SELECT COALESCE(SUM(p.unallocatedAmount), 0) FROM Payment p " +
            "WHERE p.customer.id = :customerId AND " +
            "p.tenantId = :tenantId AND p.unallocatedAmount > 0")
    BigDecimal findTotalAvailableCredit(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    @Query(
            value = """
                    SELECT * FROM payment p
                    WHERE p.tenant_id = :tenantId
                      AND (:id IS NULL OR p.id = :id)
                      AND (:customerId IS NULL OR p.customer_id = :customerId)
                      AND (:status IS NULL OR p.status = :status)
                      AND (:paymentMethod IS NULL OR p.payment_method = :paymentMethod)
                      AND (:paymentNumber IS NULL OR p.payment_number LIKE CONCAT('%', :paymentNumber, '%'))
                    """,
            nativeQuery = true
    )
    Page<Payment> getAllPayments(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("paymentNumber") String paymentNumber,
            Pageable pageable
    );
}
