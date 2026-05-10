package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.RefundStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Records a cash refund of unused advance balance back to the customer.
 *
 * On CLEARED:
 *   advance.availableBalance -= refundAmount
 *   If availableBalance = 0 → advance.status = REFUNDED
 *
 * Multiple partial refunds are allowed — a customer may ask for money back in parts.
 *
 * Status: PENDING → CLEARED | CANCELLED
 */
@Entity
@Table(name = "advance_refund", indexes = {
        @Index(name = "idx_advref_advance", columnList = "advance_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvanceRefund extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // RFD-2025-00001
    @Column(name = "refund_number", nullable = false, unique = true, length = 40)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_id", nullable = false)
    private CustomerAdvance advance;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refund_date", nullable = false)
    private Date refundDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 50)
    private PaymentMethod refundMethod; // How money is returned (CASH, CHEQUE, BANK_TRANSFER, UPI)

    @Column(name = "refund_reference_number", length = 100)
    private String refundReferenceNumber; // Cheque no / UTR

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
