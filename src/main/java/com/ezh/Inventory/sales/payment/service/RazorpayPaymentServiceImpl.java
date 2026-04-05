package com.ezh.Inventory.sales.payment.service;

import com.ezh.Inventory.config.RazorpayConfig;
import com.ezh.Inventory.sales.payment.dto.*;
import com.ezh.Inventory.sales.payment.entity.Payment;
import com.ezh.Inventory.sales.payment.entity.PaymentMethod;
import com.ezh.Inventory.sales.payment.entity.PaymentStatus;
import com.ezh.Inventory.sales.payment.repository.PaymentRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.*;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import com.razorpay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Override
    public RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto request) throws CommonException {
        validateOrderRequest(request);

        return switch (request.getPaymentMethod()) {
            case QR -> createQrOrder(request);
            case PAYMENT_LINK -> createPaymentLink(request);
            default -> createStandardOrder(request); // CHECKOUT, UPI, NET_BANKING
        };
    }

    private RazorpayOrderResponseDto createStandardOrder(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());

            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", amountInPaise);
            orderReq.put("currency", razorpayConfig.getCurrency());
            orderReq.put("receipt", "rcpt_" + System.currentTimeMillis());
            orderReq.put("payment_capture", 1); // auto-capture

            if (request.getNotes() != null) {
                JSONObject notes = new JSONObject();
                notes.put("customer_id", request.getCustomerId().toString());
                notes.put("note", request.getNotes());
                orderReq.put("notes", notes);
            }

            Order order = razorpayClient.orders.create(orderReq);
            log.info("Razorpay order created: {}", order.get("id").toString());

            return RazorpayOrderResponseDto.builder()
                    .orderId(order.get("id"))
                    .currency(order.get("currency"))
                    .amountInPaise(amountInPaise)
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .status(order.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new CommonException("Failed to create payment order: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Creates a Razorpay Payment Link.
     * The returned short URL can be shared with the customer via WhatsApp, SMS, or
     * copy-paste.
     * The link expires in 24 hours.
     */
    private RazorpayOrderResponseDto createPaymentLink(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());
            long expireBy = Instant.now().plusSeconds(86400).getEpochSecond(); // 24-hour window

            JSONObject linkReq = new JSONObject();
            linkReq.put("amount", amountInPaise);
            linkReq.put("currency", razorpayConfig.getCurrency());
            linkReq.put("expire_by", expireBy);
            linkReq.put("reminder_enable", true);

            if (request.getNotes() != null) {
                JSONObject notes = new JSONObject();
                notes.put("customer_id", request.getCustomerId().toString());
                notes.put("note", request.getNotes());
                linkReq.put("notes", notes);
            }

            // Notify via SMS only (email is optional — set false to avoid extra config)
            JSONObject notify = new JSONObject();
            notify.put("sms", false);
            notify.put("email", false);
            linkReq.put("notify", notify);

            PaymentLink paymentLink = razorpayClient.paymentLink.create(linkReq);
            String linkId = paymentLink.get("id");
            String shortUrl = paymentLink.get("short_url");
            log.info("Razorpay Payment Link created: id={}, url={}", linkId, shortUrl);

            return RazorpayOrderResponseDto.builder()
                    .orderId(linkId)
                    .currency(razorpayConfig.getCurrency())
                    .amountInPaise(amountInPaise)
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .paymentLinkId(linkId)
                    .paymentLinkUrl(shortUrl)
                    .status(paymentLink.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay Payment Link", e);
            throw new CommonException("Failed to create payment link: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private RazorpayOrderResponseDto createQrOrder(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());
            long closeBy = Instant.now().plusSeconds(3600).getEpochSecond(); // 1-hour window

            JSONObject qrReq = new JSONObject();
            qrReq.put("type", "upi_qr");
            qrReq.put("name", "EZH Payment QR");
            qrReq.put("usage", "single_use");
            qrReq.put("fixed_amount", true);
            qrReq.put("payment_amount", amountInPaise);
            qrReq.put("close_by", closeBy);

            JSONObject notes = new JSONObject();
            notes.put("customer_id", request.getCustomerId().toString());
            qrReq.put("notes", notes);

            QrCode qrCode = razorpayClient.qrCode.create(qrReq);
            log.info("Razorpay QR code created: {}", qrCode.get("id").toString());

            return RazorpayOrderResponseDto.builder()
                    .orderId(qrCode.get("id")) // QR ID acts as order reference
                    .currency(razorpayConfig.getCurrency())
                    .amountInPaise(amountInPaise)
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .qrCodeId(qrCode.get("id"))
                    .qrImageUrl(qrCode.get("image_url"))
                    .status(qrCode.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay QR code", e);
            throw new CommonException("Failed to create QR code: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    @Transactional
    public CommonResponse<?> verifyAndRecordPayment(RazorpayVerifyRequestDto request) throws CommonException {
        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        // Close QR code if this was a QR payment
        if (request.getQrCodeId() != null) {
            closeQrCode(request.getQrCodeId());
        }

        // Cancel the payment link so the customer can't pay again
        if (request.getPaymentLinkId() != null) {
            cancelPaymentLink(request.getPaymentLinkId());
        }

        // Record payment in our system via existing PaymentService
        PaymentCreateDto createDto = PaymentCreateDto.builder()
                .customerId(request.getCustomerId())
                .totalAmount(request.getAmount())
                .paymentMethod(PaymentMethod.RAZOR_PAY)
                .referenceNumber(request.getRazorpayPaymentId())
                .remarks(buildRemarks(request))
                .allocations(request.getAllocations() != null ? request.getAllocations() : new ArrayList<>())
                .build();

        return paymentService.recordPayment(createDto);
    }

    private void verifySignature(String orderId, String paymentId, String signature) throws CommonException {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            Utils.verifyPaymentSignature(attributes, razorpayConfig.getKeySecret());
            log.info("Razorpay signature verified for payment: {}", paymentId);

        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification failed for payment {}: {}", paymentId, e.getMessage());
            throw new CommonException("Payment signature verification failed — possible tampered request",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private void closeQrCode(String qrCodeId) {
        try {
            razorpayClient.qrCode.close(qrCodeId);
            log.info("Closed QR code: {}", qrCodeId);
        } catch (RazorpayException e) {
            // Non-fatal — log and continue; QR will expire naturally
            log.warn("Could not close QR code {}: {}", qrCodeId, e.getMessage());
        }
    }

    private void cancelPaymentLink(String paymentLinkId) {
        try {
            JSONObject cancelReq = new JSONObject();
            cancelReq.put("cancelled", true);
            razorpayClient.paymentLink.cancel(paymentLinkId);
            log.info("Cancelled Payment Link: {}", paymentLinkId);
        } catch (RazorpayException e) {
            // Non-fatal — link will expire naturally after 24 hours
            log.warn("Could not cancel Payment Link {}: {}", paymentLinkId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public CommonResponse<?> handleWebhook(String payload, String signature) throws CommonException {
        verifyWebhookSignature(payload, signature);

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");
        log.info("Received Razorpay webhook: {}", eventType);

        switch (eventType) {
            case "payment.captured" -> handlePaymentCaptured(event);
            case "payment.failed" -> handlePaymentFailed(event);
            case "qr_code.credited" -> handleQrCredited(event);
            default -> log.debug("Unhandled Razorpay event: {}", eventType);
        }

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Webhook processed")
                .build();
    }

    private void verifyWebhookSignature(String payload, String signature) throws CommonException {
        try {
            Utils.verifyWebhookSignature(payload, signature, razorpayConfig.getWebhookSecret());
        } catch (RazorpayException e) {
            log.warn("Razorpay webhook signature invalid: {}", e.getMessage());
            throw new CommonException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }
    }

    private void handlePaymentCaptured(JSONObject event) {
        JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");
        long paise = payment.getLong("amount");
        log.info("payment.captured — paymentId={}, orderId={}, amount=₹{}", paymentId, orderId, paise / 100.0);

        // Mark any PENDING payment record as COMPLETED if we have a draft from order
        // creation
        paymentRepository.findByReferenceNumber(orderId).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.COMPLETED);
                p.setReferenceNumber(paymentId); // update to actual payment ID
                paymentRepository.save(p);
                log.info("Updated payment {} to COMPLETED via webhook", p.getPaymentNumber());
            }
        });
    }

    private void handlePaymentFailed(JSONObject event) {
        JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String paymentId = payment.getString("id");
        String errorDesc = payment.optString("error_description", "Unknown error");
        log.warn("payment.failed — paymentId={}, reason={}", paymentId, errorDesc);

        paymentRepository.findByReferenceNumber(paymentId).ifPresent(p -> {
            p.setStatus(PaymentStatus.CANCELLED);
            p.setRemarks((p.getRemarks() != null ? p.getRemarks() + " | " : "") + "Failed: " + errorDesc);
            paymentRepository.save(p);
        });
    }

    private void handleQrCredited(JSONObject event) {
        JSONObject qr = event.getJSONObject("payload").getJSONObject("qr_code").getJSONObject("entity");
        String qrId = qr.getString("id");
        long paise = qr.getLong("payments_amount_received");
        log.info("qr_code.credited — qrId={}, amount=₹{}", qrId, paise / 100.0);
        // QR payments are self-contained; the payment record is created during /verify.
        // This event is informational unless you're implementing server-side-only QR
        // flows.
    }

    private void validateOrderRequest(RazorpayOrderRequestDto request) {
        if (request.getPaymentMethod() == RazorpayPaymentMethod.UPI
                && (request.getUpiId() == null || request.getUpiId().isBlank())) {
            throw new BadRequestException("UPI ID is required for UPI payment method");
        }
        if (request.getPaymentMethod() == RazorpayPaymentMethod.NET_BANKING
                && (request.getBankCode() == null || request.getBankCode().isBlank())) {
            throw new BadRequestException("Bank code is required for Net Banking payment method");
        }
    }

    /**
     * Converts rupees (BigDecimal) → paise (long).
     */
    private long toP(BigDecimal rupees) {
        return rupees.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private String buildRemarks(RazorpayVerifyRequestDto request) {
        String base = "Razorpay Payment | Order: " + request.getRazorpayOrderId()
                + " | Payment: " + request.getRazorpayPaymentId();
        return request.getRemarks() != null ? base + " | " + request.getRemarks() : base;
    }
}
