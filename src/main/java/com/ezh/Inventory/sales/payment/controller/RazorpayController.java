package com.ezh.Inventory.sales.payment.controller;

import com.ezh.Inventory.sales.payment.dto.RazorpayOrderRequestDto;
import com.ezh.Inventory.sales.payment.dto.RazorpayOrderResponseDto;
import com.ezh.Inventory.sales.payment.dto.RazorpayVerifyRequestDto;
import com.ezh.Inventory.sales.payment.service.RazorpayPaymentService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/razorpay")
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayPaymentService razorpayPaymentService;

    /**
     * Step 1 — Create a Razorpay order.
     * <p>
     * Supports three payment methods:
     * <ul>
     *   <li>UPI  — pass {@code upiId}</li>
     *   <li>QR   — returns {@code qrImageUrl} to display to the customer</li>
     *   <li>NET_BANKING — pass {@code bankCode} (e.g. "SBIN", "HDFC")</li>
     * </ul>
     * The frontend uses the returned {@code orderId} + {@code razorpayKeyId}
     * to initialise the Razorpay checkout / Razorpay.js.
     */
    @PostMapping(value = "/order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<RazorpayOrderResponseDto> createOrder(
            @Valid @RequestBody RazorpayOrderRequestDto request) throws CommonException {
        log.info("Creating Razorpay order for customer {} via {}", request.getCustomerId(), request.getPaymentMethod());
        RazorpayOrderResponseDto response = razorpayPaymentService.createOrder(request);
        return ResponseResource.success(HttpStatus.CREATED, response, "Razorpay order created successfully");
    }

    /**
     * Step 2 — Verify payment signature and record the payment.
     * <p>
     * Call this after the Razorpay checkout completes successfully.
     * Pass the three values returned by Razorpay checkout:
     * {@code razorpayOrderId}, {@code razorpayPaymentId}, {@code razorpaySignature}.
     */
    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> verifyPayment(
            @Valid @RequestBody RazorpayVerifyRequestDto request) throws CommonException {
        log.info("Verifying Razorpay payment: orderId={}, paymentId={}",
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());
        CommonResponse<?> response = razorpayPaymentService.verifyAndRecordPayment(request);
        return ResponseResource.success(HttpStatus.OK, response, "Payment verified and recorded successfully");
    }

    /**
     * Razorpay Webhook endpoint.
     * <p>
     * Register this URL in the Razorpay Dashboard → Webhooks:
     * {@code POST https://<your-domain>/v1/razorpay/webhook}
     * <p>
     * Handles: {@code payment.captured}, {@code payment.failed}, {@code qr_code.credited}.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> handleWebhook(@RequestBody String payload,
                                                             @RequestHeader("X-Razorpay-Signature") String signature) throws CommonException {
        log.info("Received Razorpay webhook event");
        CommonResponse<?> response = razorpayPaymentService.handleWebhook(payload, signature);
        return ResponseResource.success(HttpStatus.OK, response, "Webhook processed");
    }
}
