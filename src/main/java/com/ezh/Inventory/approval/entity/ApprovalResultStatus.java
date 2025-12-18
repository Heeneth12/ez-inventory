package com.ezh.Inventory.approval.entity;

public enum ApprovalResultStatus {
    AUTO_APPROVED,        // No approval required
    APPROVAL_REQUIRED,    // Approval created → flow must pause
    REJECTED              // (Optional future use)
}
