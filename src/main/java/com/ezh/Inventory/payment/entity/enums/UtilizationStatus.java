package com.ezh.Inventory.payment.entity.enums;

public enum UtilizationStatus {
    CONFIRMED,  // Applied to invoice — invoice balance reduced
    REVERSED    // Invoice was cancelled — advance/CN balance restored
}
