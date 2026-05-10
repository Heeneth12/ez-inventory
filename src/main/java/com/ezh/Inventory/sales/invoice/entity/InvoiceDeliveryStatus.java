package com.ezh.Inventory.sales.invoice.entity;

public enum InvoiceDeliveryStatus {
    DRAFT,
    PENDING,        // Not yet delivered
    IN_PROGRESS,    // Moved to delivery
    DELIVERED,
    CANCELLED,
    CANCEL_DELIVERY,    // cancel delivery in sap
}
