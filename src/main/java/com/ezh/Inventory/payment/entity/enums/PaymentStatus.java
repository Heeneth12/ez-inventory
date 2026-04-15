package com.ezh.Inventory.payment.entity.enums;

public enum PaymentStatus {
    DRAFT,      // Saved but not yet submitted/confirmed
    CONFIRMED,  // Fully allocated to invoices — revenue recognized
    CANCELLED   // Voided — invoice.paidAmount reversed
}
