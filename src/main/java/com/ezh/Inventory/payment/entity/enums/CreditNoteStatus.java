package com.ezh.Inventory.payment.entity.enums;

public enum CreditNoteStatus {
    ISSUED,              // Auto-created from sales return — availableBalance = amount
    PARTIALLY_UTILIZED,  // Some credit applied to invoices
    FULLY_UTILIZED,      // availableBalance = 0 via utilizations
    REFUNDED,            // availableBalance = 0 — paid back as cash
    CANCELLED            // Voided
}
