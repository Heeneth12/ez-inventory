package com.ezh.Inventory.purchase.grn.entity;

public enum GrnStatus {
    PENDING_QA, // Truck unloaded, checking quality
    APPROVED,   // Stock Updated (IN)
    CANCELLED
}