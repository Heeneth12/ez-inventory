package com.ezh.Inventory.payment.entity.enums;

public enum PaymentType {
    REGULAR,      // PAY- prefix: money received and optionally allocated to invoices
    ADVANCE,      // ADV- prefix: money received upfront with no invoice at time of payment
    CREDIT_NOTE   // CN-  prefix: auto-generated credit when a sales return is processed
}

