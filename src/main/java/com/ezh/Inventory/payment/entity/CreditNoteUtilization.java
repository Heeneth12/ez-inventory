package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.UtilizationStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Records a chunk of credit note value being applied to a specific invoice.
 * Same mechanics as AdvanceUtilization but sourced from a CreditNote.
 */
@Entity
@Table(name = "credit_note_utilization", indexes = {
        @Index(name = "idx_cnu_cn",      columnList = "credit_note_id"),
        @Index(name = "idx_cnu_invoice", columnList = "invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteUtilization extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "utilized_amount", nullable = false)
    private BigDecimal utilizedAmount;

    @Column(name = "utilization_date", nullable = false)
    private Date utilizationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UtilizationStatus status; // CONFIRMED | REVERSED

    @Column(name = "remarks", length = 500)
    private String remarks;
}
