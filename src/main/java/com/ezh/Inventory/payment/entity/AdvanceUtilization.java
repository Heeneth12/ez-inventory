package com.ezh.Inventory.payment.entity;

import com.ezh.Inventory.payment.entity.enums.UtilizationStatus;
import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Records a chunk of advance balance being applied to a specific invoice.
 *
 * On CONFIRM:
 *   advance.availableBalance -= utilizedAmount
 *   invoice.paidAmount       += utilizedAmount
 *
 * On REVERSED (invoice cancelled):
 *   advance.availableBalance += utilizedAmount   (balance restored)
 *   invoice.paidAmount       -= utilizedAmount
 */
@Entity
@Table(name = "advance_utilization")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvanceUtilization extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_id", nullable = false)
    private CustomerAdvance advance;

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
