package com.ezh.Inventory.sales.order.entity;

public enum SalesOrderStatus {
    CREATED,            // Draft / Open
    PENDING_APPROVAL,
    REJECTED,
    CONFIRMED,
    PARTIALLY_INVOICED, // Some items billed
    FULLY_INVOICED,     // All items billed
    CANCELLED,          // Dead order
}

