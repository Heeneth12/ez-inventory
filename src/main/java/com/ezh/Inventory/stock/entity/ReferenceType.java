package com.ezh.Inventory.stock.entity;

public enum ReferenceType {
    GRN,                // Purchase Goods Receipt Note
    SALE,               // Sales / Invoice / POS Order
    PURCHASE_RETURN,    // Return back to supplier
    CANCEL_DELIVERY,    // cancel delivery in sap
    SALES_RETURN,       // Customer returned goods
    TRANSFER,           // Stock movement between warehouses
    PRODUCTION,         // Raw materials consumed / finished goods produced
    ADJUSTMENT,         // Manual physical stock adjustment
    OPENING_STOCK,      // First-time opening quantity entry
    RESERVATION,        // Stock reserved for an order (not reduced yet)
    CONSUMPTION,        // Stock consumed without sales (office use, samples)
    WRITE_OFF           // Permanent removal for accounting purpose

}