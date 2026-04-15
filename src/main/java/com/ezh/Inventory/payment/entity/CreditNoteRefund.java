package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.PaymentMethod;
import com.ezh.Inventory.payment.entity.enums.RefundStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Records the business paying back a credit note's value as cash to the customer.
 * This converts a non-cash liability (returned goods) into a cash outflow.
 *
 * Status: PENDING → CLEARED | CANCELLED
 */
@Entity
@Table(name = "credit_note_refund", indexes = {
        @Index(name = "idx_cnref_cn", columnList = "credit_note_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteRefund extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // RFD-2025-00001
    @Column(name = "refund_number", nullable = false, unique = true, length = 40)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refund_date", nullable = false)
    private Date refundDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 50)
    private PaymentMethod refundMethod; // CASH | CHEQUE | BANK_TRANSFER | UPI

    @Column(name = "refund_reference_number", length = 100)
    private String refundReferenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
