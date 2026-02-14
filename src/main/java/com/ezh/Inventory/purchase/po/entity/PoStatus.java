package com.ezh.Inventory.purchase.po.entity;

public enum PoStatus {
    DRAFT,              // Editing
    ISSUED,             // Sent to Supplier
    PARTIALLY_RECEIVED, // Received 50/100
    COMPLETED,          // Received 100/100
    CANCELLED,           // Deal off
    PENDING,
    ASN_CONFIRMED,
    ASN_PENDING
}
