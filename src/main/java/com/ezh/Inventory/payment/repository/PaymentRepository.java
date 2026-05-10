package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.dto.PaymentStats;
import com.ezh.Inventory.payment.entity.Payment;
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
            @Param("paymentType") String paymentType,  // kept for API compat, ignored in query
            Pageable pageable
    );


    Optional<Payment> findByReferenceNumber(String referenceNumber);

    @Query("SELECT " +
            "COUNT(p.id) as totalCount, " +
            "SUM(p.amount) as totalCollected, " +
            "SUM(CASE WHEN p.status = 'PENDING' THEN p.amount ELSE 0 END) as pendingAmount " +
            "FROM Payment p " +
            "WHERE p.tenantId = :tenantId AND p.isDeleted = false")
    PaymentStats getPaymentStats(@Param("tenantId") Long tenantId);
}
