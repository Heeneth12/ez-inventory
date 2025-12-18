package com.ezh.Inventory.approval.entity;

import com.ezh.Inventory.utils.common.CommonSerializable; // Assuming this exists
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "approval_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest extends CommonSerializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // What kind of approval is this? (e.g., HIGH_VALUE_INVOICE)
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", length = 50, nullable = false)
    private ApprovalType approvalType;

    // --- Link to the Source ---
    // The Primary Key of the Invoice, PO, or Refund
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    // Readable ID for display (e.g., "INV-2024-001", "PO-992")
    @Column(name = "reference_code")
    private String referenceCode;

    // --- Status Tracking ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    // The user who tried to create the Invoice/PO
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    // --- Context Data ---

    // Why was this triggered?
    // e.g., "Discount 15% exceeds limit 10%"
    @Column(name = "description")
    private String description;

    // The specific value that caused the trigger
    // e.g., 150000.00 (The invoice amount) or 15.0 (The discount %)
    @Column(name = "value_amount")
    private BigDecimal valueAmount;

    // User ID of the manager who clicked Approve/Reject
    @Column(name = "actioned_by")
    private Long actionedBy;

    // Remarks from the manager
    // e.g., "Approved this time, but stick to policy next time."
    @Column(name = "action_remarks")
    private String actionRemarks;
}