package com.ezh.Inventory.approval.entity;

public enum ApprovalType {
    // Value Based Checks
    HIGH_VALUE_INVOICE,     // Check against thresholdAmount (e.g., Bill > 100,000)
    PO_APPROVAL,            // Check against thresholdAmount (e.g., PO > 50,000)
    STOCK_ADJUSTMENT,       // Check against thresholdAmount (e.g., Adjusting > $1000 value)

    // Percentage Based Checks
    SALES_ORDER_DISCOUNT,
    INVOICE_DISCOUNT,       // Check against thresholdPercentage (e.g., Discount > 10%)
    TAX_VARIANCE,           // Check against thresholdPercentage (e.g., Tax differs by > 1%)

    // Absolute Checks (Always require approval if enabled)
    SALES_REFUND,
    ADVANCE_REFUND
}