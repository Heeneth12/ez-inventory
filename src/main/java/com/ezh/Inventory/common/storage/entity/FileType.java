package com.ezh.Inventory.common.storage.entity;

/**
 * Classifies every file stored in S3.
 * Used as part of the S3 key path: {tenantId}/{referenceType}/{referenceId}/{fileType}/{uuid}_{filename}
 */
public enum FileType {

    // ── Item / Product ────────────────────────────────────────────────────────
    ITEM_IMAGE,
    ITEM_DOCUMENT,

    // ── Purchase Workflow ─────────────────────────────────────────────────────
    PURCHASE_REQUEST_PDF,
    PURCHASE_ORDER_PDF,
    PURCHASE_ORDER_DOCUMENT,
    GOODS_RECEIPT_PDF,
    GOODS_RECEIPT_DOCUMENT,
    PURCHASE_RETURN_PDF,
    PURCHASE_RETURN_DOCUMENT,

    // ── Sales Workflow ────────────────────────────────────────────────────────
    SALES_ORDER_PDF,
    SALES_ORDER_DOCUMENT,
    INVOICE_PDF,
    INVOICE_DOCUMENT,
    DELIVERY_NOTE_PDF,
    DELIVERY_DOCUMENT,
    PAYMENT_RECEIPT_PDF,
    PAYMENT_DOCUMENT,
    SALES_RETURN_PDF,
    SALES_RETURN_DOCUMENT,

    // ── Stock / Inventory ─────────────────────────────────────────────────────
    STOCK_ADJUSTMENT_DOCUMENT,

    // ── People / Contacts ─────────────────────────────────────────────────────
    EMPLOYEE_PHOTO,
    EMPLOYEE_DOCUMENT,
    CONTACT_DOCUMENT,
    CONTACT_LOGO,

    // ── Exports ───────────────────────────────────────────────────────────────
    EXCEL_EXPORT,
    PDF_REPORT,

    // ── Catch-all ─────────────────────────────────────────────────────────────
    GENERAL_DOCUMENT
}
