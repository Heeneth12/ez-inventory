package com.ezh.Inventory.sales.payment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponseDto {

    /** Razorpay order ID — pass this to the frontend Razorpay checkout */
    private String orderId;

    private String currency;

    /** Amount in paise (Razorpay's unit; 100 paise = ₹1) */
    private Long amountInPaise;

    /** Razorpay public key ID — needed by frontend to initialize checkout */
    private String razorpayKeyId;

    /** Populated only for QR method — the image URL to display */
    private String qrImageUrl;

    /** Populated only for QR method — used to close the QR after payment */
    private String qrCodeId;

    /** Populated only for PAYMENT_LINK method — short URL to share with the customer */
    private String paymentLinkUrl;

    /** Populated only for PAYMENT_LINK method — Razorpay payment link ID */
    private String paymentLinkId;

    private String status;
}
