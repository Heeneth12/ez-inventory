package com.ezh.Inventory.payment.entity.enums;

public enum RefundStatus {
    PENDING,    // Refund initiated — cheque written or transfer initiated
    CLEARED,    // Customer confirmed receipt of money
    CANCELLED   // Refund cancelled before it cleared
}
