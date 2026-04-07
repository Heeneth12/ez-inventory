package com.ezh.Inventory.sales.payment.dto;

public enum RazorpayPaymentMethod {
    /** Opens the Razorpay checkout popup — customer picks UPI / Card / Net Banking / Wallet */
    CHECKOUT,
    /** Razorpay QR code — customer scans and pays via any UPI app */
    QR,
    /** Razorpay Payment Link — sharable URL sent to the customer */
    PAYMENT_LINK,
    /** Standard UPI collect request — legacy */
    UPI,
    /** Net Banking — legacy */
    NET_BANKING
}
