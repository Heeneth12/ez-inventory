package com.ezh.Inventory.common.storage.entity;

/**
 * Identifies which domain entity owns a stored file.
 * Maps 1-to-1 with the modules in this inventory application.
 * Used as path segment in the S3 key and for repository queries.
 */
public enum FileReferenceType {

    ITEM,
    // Purchase
    PURCHASE_REQUEST,
    PURCHASE_ORDER,
    GOODS_RECEIPT_NOTE,
    PURCHASE_RETURN,

    // Sales
    SALES_ORDER,
    INVOICE,
    DELIVERY,
    PAYMENT,
    SALES_RETURN,

    // Stock
    STOCK_ADJUSTMENT,

    // People / Network
    EMPLOYEE,
    CONTACT,

    // Tenant
    TENANT,

    GENERAL
}
