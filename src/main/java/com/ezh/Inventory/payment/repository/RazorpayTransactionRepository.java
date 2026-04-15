package com.ezh.Inventory.payment.repository;

import com.ezh.Inventory.payment.entity.RazorpayTransaction;
import com.ezh.Inventory.payment.entity.enums.RazorpayTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RazorpayTransactionRepository extends JpaRepository<RazorpayTransaction, Long> {

    Optional<RazorpayTransaction> findByRazorpayResourceId(String razorpayResourceId);

    Page<RazorpayTransaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<RazorpayTransaction> findByTenantIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, RazorpayTransactionStatus status, Pageable pageable);

    Page<RazorpayTransaction> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            Long tenantId, Long customerId, Pageable pageable);

    /** Find all Razorpay transactions linked to a specific invoice ID. */
    @Query("SELECT t FROM RazorpayTransaction t WHERE t.tenantId = :tenantId " +
           "AND (t.invoiceIds = :invoiceId OR t.invoiceIds LIKE CONCAT(:invoiceId, ',%') " +
           "OR t.invoiceIds LIKE CONCAT('%,', :invoiceId, ',%') " +
           "OR t.invoiceIds LIKE CONCAT('%,', :invoiceId)) " +
           "ORDER BY t.createdAt DESC")
    Page<RazorpayTransaction> findByTenantIdAndInvoiceId(
            @Param("tenantId") Long tenantId,
            @Param("invoiceId") String invoiceId,
            Pageable pageable);
}
