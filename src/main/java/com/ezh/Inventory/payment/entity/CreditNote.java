package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.CreditNoteStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Auto-generated when a SalesReturn is processed.
 * Represents the value the business owes the customer due to returned goods.
 * This is a LIABILITY — never backed by cash at creation time.
 *
 * Key difference from CustomerAdvance:
 *   - Advance: customer gave cash upfront
 *   - CreditNote: customer returned goods — business owes the value
 *
 * Lifecycle: ISSUED → PARTIALLY_UTILIZED → FULLY_UTILIZED | REFUNDED | CANCELLED
 *
 * Invariant: availableBalance = amount − sum(CONFIRMED utilizations) − sum(CLEARED refunds)
 */
@Entity
@Table(name = "credit_note", indexes = {
        @Index(name = "idx_cn_customer", columnList = "tenant_id,customer_id"),
        @Index(name = "idx_cn_number",   columnList = "tenant_id,credit_note_number"),
        @Index(name = "idx_cn_return",   columnList = "source_return_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNote extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // CN-2025-00001
    @Column(name = "credit_note_number", nullable = false, unique = true, length = 40)
    private String creditNoteNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    // The SalesReturn that triggered this CN — mandatory, always traceable
    @Column(name = "source_return_id", nullable = false)
    private Long sourceReturnId;

    @Column(name = "issue_date", nullable = false)
    private Date issueDate;

    // Value of returned goods — immutable after ISSUED
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    // Running balance — updated on every utilization or refund
    @Builder.Default
    @Column(name = "available_balance", nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreditNoteStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNoteUtilization> utilizations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNoteRefund> refunds = new ArrayList<>();
}
