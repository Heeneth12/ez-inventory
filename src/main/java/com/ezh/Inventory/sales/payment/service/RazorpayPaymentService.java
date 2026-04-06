package com.ezh.Inventory.sales.payment.service;

import com.ezh.Inventory.sales.payment.dto.PaymentLinkEmailRequestDto;
import com.ezh.Inventory.sales.payment.dto.RazorpayOrderRequestDto;
import com.ezh.Inventory.sales.payment.dto.RazorpayOrderResponseDto;
import com.ezh.Inventory.sales.payment.dto.RazorpayVerifyRequestDto;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.exception.CommonException;

public interface RazorpayPaymentService {

    /**
     * Creates a Razorpay order for UPI, QR or Net Banking.
     * Returns the order ID and (for QR) the QR image URL.
     */
    RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto request) throws CommonException;

    /**
     * Verifies the Razorpay payment signature and, on success,
     * records the payment in our system.
     */
    CommonResponse<?> verifyAndRecordPayment(RazorpayVerifyRequestDto request) throws CommonException;

    /**
     * Handles incoming Razorpay webhook events.
     *
     * @param payload   raw JSON body from Razorpay
     * @param signature value of the X-Razorpay-Signature header
     */
    CommonResponse<?> handleWebhook(String payload, String signature) throws CommonException;


    /**
     * Polls the live status of a Razorpay resource.
     * Accepts a QR-code ID ({@code qr_*}) or a Payment-Link ID ({@code plink_*}).
     * Returns {@code { "orderId": "...", "status": "CREATED" | "PAID" }}.
     */
    CommonResponse<?> checkOrderStatus(String orderId) throws CommonException;
}
