package com.ezh.Inventory.approval.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "approval_config",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "approval_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalConfig extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "approval_type", columnDefinition = "approval_type")
    private ApprovalType approvalType;

    @Column(name = "is_enabled")
    @Builder.Default
    private boolean isEnabled = true;

    // --- The Rules ---
    // Use this for Invoice Value, PO Value, Stock Value
    // e.g., If Invoice > 10000.00
    @Column(name = "threshold_amount")
    private BigDecimal thresholdAmount;

    // Use this for Discounts, Tax variance
    // e.g., If Discount > 10.0 (percent)
    @Column(name = "threshold_percentage")
    private Double thresholdPercentage;

    // --- Role Management ---
    // Who is allowed to approve this?
    // e.g., "MANAGER", "ADMIN", "SUPERVISOR"
    @Column(name = "approver_role")
    private String approverRole;
}