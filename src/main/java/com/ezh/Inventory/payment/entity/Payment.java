package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.PaymentStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "payment_number", nullable = false, unique = true, length = 40)
    private String paymentNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "payment_date", nullable = false)
    private Date paymentDate;

    // Total received — MUST equal sum(allocations.allocatedAmount). Immutable after CONFIRMED.
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod; // CASH | UPI | CHEQUE | BANK_TRANSFER | RAZOR_PAY ...

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status; // DRAFT → CONFIRMED → CANCELLED

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // Cheque no / UTR / UPI ref / Razorpay ID

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "remarks", length = 500)
    private String remarks;

    // Which invoices does this payment cover?
    // sum(allocations.allocatedAmount) == this.amount — always
    @Builder.Default
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAllocation> allocations = new ArrayList<>();
}
