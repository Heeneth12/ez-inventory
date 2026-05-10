package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.AdvanceStatus;
import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents money deposited by a customer BEFORE an invoice is raised.
 * This is a LIABILITY — the business owes the customer either goods or a refund.
 *
 * Lifecycle: DRAFT → CONFIRMED → PARTIALLY_UTILIZED → FULLY_UTILIZED | REFUNDED | CANCELLED
 *
 * Invariant: availableBalance = amount − sum(CONFIRMED utilizations) − sum(CLEARED refunds)
 */
@Entity
@Table(name = "customer_advance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAdvance extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // ADV-2025-00001
    @Column(name = "advance_number", nullable = false, unique = true, length = 40)
    private String advanceNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "received_date", nullable = false)
    private Date receivedDate;

    // Original amount deposited — immutable after CONFIRMED
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    // Denormalized running balance: updated atomically on every utilization or refund
    // availableBalance = amount − sum(CONFIRMED utilizations) − sum(CLEARED refunds)
    @Builder.Default
    @Column(name = "available_balance", nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod; // How the customer paid (CASH, UPI, CHEQUE, ...)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AdvanceStatus status;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // Cheque no / UTR / UPI ref

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "advance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdvanceUtilization> utilizations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "advance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdvanceRefund> refunds = new ArrayList<>();
}
