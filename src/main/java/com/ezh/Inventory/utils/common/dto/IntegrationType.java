package com.ezh.Inventory.utils.common.dto;

public enum IntegrationType {
    // Payment Gateways
    RAZORPAY,
    RAZORPAY_TEST,
    STRIPE,
    STRIPE_TEST,

    // Communication
    WHATSAPP,
    SLACK,

    // CRM / Productivity
    ZOHO,

    // Email
    SENDGRID,
    EMAIL_SMTP,

    // Generic
    WEBHOOK_GENERIC
}
