package com.ezh.Inventory.purchase.prq.entity;

public enum PrqStatus {
    APPROVAL_PENDING, // Awaiting managerial approval
    PENDING,    // Waiting for approval
    APPROVED,   // Ready to be converted to PO
    REJECTED,   // Denied by management
    CONVERTED,  // Already turned into a PO
    DRAFT
}
