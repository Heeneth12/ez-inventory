package com.ezh.Inventory.payment.entity.enums;

public enum AdvanceStatus {
    DRAFT,               // Saved but not yet confirmed
    CONFIRMED,           // Money received — availableBalance = amount
    PARTIALLY_UTILIZED,  // Some balance consumed against invoices
    FULLY_UTILIZED,      // availableBalance = 0 via utilizations only
    REFUNDED,            // availableBalance = 0 via refund(s)
    CANCELLED            // Voided before any utilization
}
