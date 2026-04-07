package com.ezh.Inventory.sales.payment.service;

import com.ezh.Inventory.config.RazorpayConfig;
import com.ezh.Inventory.notifications.common.dto.NotificationDistributor;
import com.ezh.Inventory.notifications.common.dto.NotificationRequest;
import com.ezh.Inventory.notifications.common.entity.NotificationChannel;
import com.ezh.Inventory.notifications.common.entity.NotificationType;
import com.ezh.Inventory.notifications.common.entity.TargetType;
import com.ezh.Inventory.notifications.common.service.NotificationService;
import com.ezh.Inventory.sales.payment.dto.*;
import com.ezh.Inventory.sales.payment.entity.PaymentMethod;
import com.ezh.Inventory.sales.payment.entity.RazorpayTransaction;
import com.ezh.Inventory.sales.payment.entity.RazorpayTransactionPurpose;
import com.ezh.Inventory.sales.payment.entity.RazorpayTransactionStatus;
import com.ezh.Inventory.sales.payment.repository.RazorpayTransactionRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.*;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final String DEV_EMAIL = "heeneth123@gmail.com";

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentService paymentService;
    private final RazorpayTransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;


    @Override
    public RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto request) throws CommonException {
        validateOrderRequest(request);

        RazorpayOrderResponseDto response = switch (request.getPaymentMethod()) {
            case QR -> createQrOrder(request);
            case PAYMENT_LINK -> createPaymentLink(request);
            default -> createStandardOrder(request); // CHECKOUT, UPI, NET_BANKING
        };

        saveTransaction(request, response);
        return response;
    }

    private RazorpayOrderResponseDto createStandardOrder(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());

            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", amountInPaise);
            orderReq.put("currency", razorpayConfig.getCurrency());
            orderReq.put("receipt", "rcpt_" + System.currentTimeMillis());
            orderReq.put("payment_capture", 1);

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

    private RazorpayOrderResponseDto createPaymentLink(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());
            long expireBy = Instant.now().plusSeconds(86400).getEpochSecond();

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

            JSONObject notify = new JSONObject();
            notify.put("sms", false);
            notify.put("email", false);
            linkReq.put("notify", notify);

            PaymentLink paymentLink = razorpayClient.paymentLink.create(linkReq);
            String linkId  = paymentLink.get("id");
            String shortUrl = paymentLink.get("short_url");
            log.info("Razorpay Payment Link created: id={}, url={}", linkId, shortUrl);

            RazorpayOrderResponseDto response = RazorpayOrderResponseDto.builder()
                    .orderId(linkId)
                    .currency(razorpayConfig.getCurrency())
                    .amountInPaise(amountInPaise)
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .paymentLinkId(linkId)
                    .paymentLinkUrl(shortUrl)
                    .status(paymentLink.get("status"))
                    .build();

            dispatchPaymentLinkEmail(shortUrl, amountInPaise, request.getCustomerId(), DEV_EMAIL);
            return response;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay Payment Link", e);
            throw new CommonException("Failed to create payment link: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private RazorpayOrderResponseDto createQrOrder(RazorpayOrderRequestDto request) throws CommonException {
        try {
            long amountInPaise = toP(request.getAmount());
            long closeBy = Instant.now().plusSeconds(3600).getEpochSecond();

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
                    .orderId(qrCode.get("id"))
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

    /**
     * Saves a {@link RazorpayTransaction} record immediately after creating an order.
     * Stores tenantId and allocations so that the webhook can record the payment
     * later without requiring a JWT security context.
     */
    private void saveTransaction(RazorpayOrderRequestDto request, RazorpayOrderResponseDto response) {
        try {
            Long tenantId = UserContextUtil.getTenantId();
            List<PaymentAllocationDto> allocations = request.getAllocations();
            String allocationsJson = serializeAllocations(allocations);

            // Derive purpose and invoice ID list from allocations
            RazorpayTransactionPurpose purpose;
            String invoiceIds = null;
            if (allocations == null || allocations.isEmpty()) {
                purpose = RazorpayTransactionPurpose.ADVANCE;
            } else if (allocations.size() == 1) {
                purpose = RazorpayTransactionPurpose.INVOICE;
                invoiceIds = String.valueOf(allocations.get(0).getInvoiceId());
            } else {
                purpose = RazorpayTransactionPurpose.MULTI_INVOICE;
                invoiceIds = allocations.stream()
                        .map(a -> String.valueOf(a.getInvoiceId()))
                        .reduce((a, b) -> a + "," + b)
                        .orElse(null);
            }

            RazorpayTransaction txn = RazorpayTransaction.builder()
                    .tenantId(tenantId)
                    .customerId(request.getCustomerId())
                    .razorpayResourceId(response.getOrderId())
                    .paymentMethod(request.getPaymentMethod().name())
                    .amountInPaise(response.getAmountInPaise())
                    .status(RazorpayTransactionStatus.CREATED)
                    .purpose(purpose)
                    .invoiceIds(invoiceIds)
                    .allocationsJson(allocationsJson)
                    .notes(request.getNotes())
                    .build();

            transactionRepository.save(txn);
            log.info("RazorpayTransaction saved: resourceId={}, purpose={}, invoiceIds={}, tenantId={}",
                    response.getOrderId(), purpose, invoiceIds, tenantId);

        } catch (Exception e) {
            // Non-fatal — do not break the order-creation response
            log.error("Failed to save RazorpayTransaction for resourceId={}: {}", response.getOrderId(), e.getMessage());
        }
    }


    @Override
    @Transactional
    public CommonResponse<?> verifyAndRecordPayment(RazorpayVerifyRequestDto request) throws CommonException {
        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (request.getQrCodeId() != null) {
            closeQrCode(request.getQrCodeId());
        }
        if (request.getPaymentLinkId() != null) {
            cancelPaymentLink(request.getPaymentLinkId());
        }

        // Check idempotency — don't record twice for the same order
        transactionRepository.findByRazorpayResourceId(request.getRazorpayOrderId())
                .ifPresent(txn -> {
                    if (txn.getPaymentRecordId() != null) {
                        log.warn("Payment already recorded for resourceId={}, skipping", request.getRazorpayOrderId());
                        throw new BadRequestException("Payment already recorded for this order");
                    }
                });

        PaymentCreateDto createDto = PaymentCreateDto.builder()
                .customerId(request.getCustomerId())
                .totalAmount(request.getAmount())
                .paymentMethod(PaymentMethod.RAZOR_PAY)
                .referenceNumber(request.getRazorpayPaymentId())
                .remarks(buildRemarks(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRemarks()))
                .allocations(request.getAllocations() != null ? request.getAllocations() : new ArrayList<>())
                .build();

        CommonResponse<?> result = paymentService.recordPayment(createDto);

        // Update transaction status
        transactionRepository.findByRazorpayResourceId(request.getRazorpayOrderId())
                .ifPresent(txn -> {
                    txn.setRazorpayPaymentId(request.getRazorpayPaymentId());
                    txn.setStatus(RazorpayTransactionStatus.PAID);
                    if (result.getId() != null) {
                        txn.setPaymentRecordId(Long.parseLong(result.getId()));
                    }
                    transactionRepository.save(txn);
                    log.info("RazorpayTransaction updated to PAID: resourceId={}", request.getRazorpayOrderId());
                });

        return result;
    }


    /**
     * Polls live status from Razorpay.
     * When PAID and no Payment record exists yet, auto-records the payment so
     * the DB stays consistent even if the webhook was missed.
     */
    @Override
    @Transactional
    public CommonResponse<?> checkOrderStatus(String orderId) throws CommonException {
        try {
            String status;
            String razorpayPaymentId = null;

            if (orderId.startsWith("qr_")) {
                QrCode qrCode = razorpayClient.qrCode.fetch(orderId);
                long received = qrCode.has("payments_amount_received")
                        ? ((Number) qrCode.get("payments_amount_received")).longValue()
                        : 0L;
                status = received > 0 ? "PAID" : "CREATED";
                log.debug("QR status check: id={}, received={} paise, resolved={}", orderId, received, status);

            } else if (orderId.startsWith("plink_")) {
                PaymentLink link = razorpayClient.paymentLink.fetch(orderId);
                String rawStatus = link.get("status");
                status = "paid".equalsIgnoreCase(rawStatus) ? "PAID" : "CREATED";
                // Extract payment_id from the link if paid
                if ("PAID".equals(status) && link.has("payments")) {
                    try {
                        org.json.JSONArray paymentsArray = (org.json.JSONArray) link.get("payments");
                        if (paymentsArray != null && paymentsArray.length() > 0) {
                            razorpayPaymentId = paymentsArray.getJSONObject(0).getString("payment_id");
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract payment_id from Payment Link {}: {}", orderId, e.getMessage());
                    }
                }
                log.debug("Payment Link status check: id={}, rawStatus={}, resolved={}", orderId, rawStatus, status);

            } else {
                throw new CommonException("Unrecognised order ID format: " + orderId, HttpStatus.BAD_REQUEST);
            }

            // Auto-record payment when PAID and not yet in DB
            if ("PAID".equals(status)) {
                autoRecordIfNeeded(orderId, razorpayPaymentId);
            }

            return CommonResponse.builder()
                    .status(Status.SUCCESS)
                    .message("Status fetched")
                    .data(Map.of("orderId", orderId, "status", status))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to check Razorpay status for {}: {}", orderId, e.getMessage());
            throw new CommonException("Could not fetch order status: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Records a payment in DB if the transaction is still in CREATED state.
     * Called by status-polling when PAID is detected.
     */
    private void autoRecordIfNeeded(String resourceId, String razorpayPaymentId) {
        transactionRepository.findByRazorpayResourceId(resourceId).ifPresent(txn -> {
            if (txn.getStatus() != RazorpayTransactionStatus.CREATED) {
                log.debug("autoRecordIfNeeded: transaction {} already in state {}, skipping", resourceId, txn.getStatus());
                return;
            }
            try {
                List<PaymentAllocationDto> allocations = deserializeAllocations(txn.getAllocationsJson());
                PaymentCreateDto createDto = PaymentCreateDto.builder()
                        .tenantId(txn.getTenantId())
                        .customerId(txn.getCustomerId())
                        .totalAmount(fromPaise(txn.getAmountInPaise()))
                        .paymentMethod(PaymentMethod.RAZOR_PAY)
                        .referenceNumber(razorpayPaymentId != null ? razorpayPaymentId : resourceId)
                        .remarks(buildRemarks(resourceId, razorpayPaymentId, "Auto-recorded via status poll"))
                        .allocations(allocations)
                        .build();

                CommonResponse<?> result = paymentService.recordPayment(createDto);

                txn.setRazorpayPaymentId(razorpayPaymentId);
                txn.setStatus(RazorpayTransactionStatus.PAID);
                if (result.getId() != null) {
                    txn.setPaymentRecordId(Long.parseLong(result.getId()));
                }
                transactionRepository.save(txn);
                log.info("autoRecordIfNeeded: Payment recorded for resourceId={}, paymentRecordId={}", resourceId, result.getId());

            } catch (Exception e) {
                log.error("autoRecordIfNeeded: Failed to auto-record payment for resourceId={}: {}", resourceId, e.getMessage());
            }
        });
    }

    // ── Webhook Handling ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CommonResponse<?> handleWebhook(String payload, String signature) throws CommonException {
        verifyWebhookSignature(payload, signature);

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");
        log.info("Received Razorpay webhook: {}", eventType);

        switch (eventType) {
            case "payment.captured"    -> handlePaymentCaptured(event);
            case "payment.failed"      -> handlePaymentFailed(event);
            case "payment_link.paid"   -> handlePaymentLinkPaid(event);
            case "qr_code.credited"    -> handleQrCredited(event);
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

    /**
     * Handles {@code payment.captured} — fired for standard checkout/UPI orders.
     * If /verify was already called the transaction will be PAID; skip.
     * Otherwise, record the payment now.
     */
    private void handlePaymentCaptured(JSONObject event) {
        JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String paymentId = payment.getString("id");
        String orderId   = payment.getString("order_id");
        long paise       = payment.getLong("amount");
        log.info("payment.captured: paymentId={}, orderId={}, amount=Rs.{}", paymentId, orderId, paise / 100.0);

        transactionRepository.findByRazorpayResourceId(orderId).ifPresentOrElse(
                txn -> {
                    if (txn.getStatus() == RazorpayTransactionStatus.CREATED) {
                        recordPaymentFromTransaction(txn, paymentId, "Captured via webhook");
                    } else {
                        log.debug("payment.captured: transaction {} already {}, skipping", orderId, txn.getStatus());
                    }
                },
                () -> log.warn("payment.captured: No RazorpayTransaction found for orderId={}", orderId)
        );
    }

    /**
     * Handles {@code payment_link.paid} — fired when a customer pays via a Payment Link.
     * This is the primary success event for the PAYMENT_LINK flow.
     */
    private void handlePaymentLinkPaid(JSONObject event) {
        JSONObject paymentLink = event.getJSONObject("payload").getJSONObject("payment_link").getJSONObject("entity");
        String linkId   = paymentLink.getString("id");
        long   paise    = paymentLink.getLong("amount");

        // Extract the actual razorpay payment ID from the nested payment object
        String razorpayPaymentId = null;
        try {
            razorpayPaymentId = event.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity")
                    .getString("id");
        } catch (Exception e) {
            log.warn("payment_link.paid: Could not extract payment ID for linkId={}", linkId);
        }

        log.info("payment_link.paid: linkId={}, amount=Rs.{}, paymentId={}", linkId, paise / 100.0, razorpayPaymentId);

        final String finalPaymentId = razorpayPaymentId;
        transactionRepository.findByRazorpayResourceId(linkId).ifPresentOrElse(
                txn -> {
                    if (txn.getStatus() == RazorpayTransactionStatus.CREATED) {
                        recordPaymentFromTransaction(txn, finalPaymentId, "Paid via payment link webhook");
                    } else {
                        log.debug("payment_link.paid: transaction {} already {}, skipping", linkId, txn.getStatus());
                    }
                },
                () -> log.warn("payment_link.paid: No RazorpayTransaction found for linkId={}", linkId)
        );
    }

    /**
     * Handles {@code payment.failed} — marks the transaction as FAILED and records the error.
     */
    private void handlePaymentFailed(JSONObject event) {
        JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String paymentId  = payment.getString("id");
        String orderId    = payment.optString("order_id", null);
        String linkId     = payment.optString("payment_link_id", null);
        String errorDesc  = payment.optString("error_description", "Unknown error");
        log.warn("payment.failed: paymentId={}, orderId={}, linkId={}, reason={}", paymentId, orderId, linkId, errorDesc);

        // Try to find by orderId first, then by linkId
        String resourceId = orderId != null ? orderId : linkId;
        if (resourceId == null) {
            log.warn("payment.failed: Cannot determine resource ID for paymentId={}", paymentId);
            return;
        }

        transactionRepository.findByRazorpayResourceId(resourceId).ifPresentOrElse(
                txn -> {
                    txn.setStatus(RazorpayTransactionStatus.FAILED);
                    txn.setRazorpayPaymentId(paymentId);
                    txn.setErrorDescription(errorDesc);
                    transactionRepository.save(txn);
                    log.info("RazorpayTransaction updated to FAILED: resourceId={}, reason={}", resourceId, errorDesc);
                },
                () -> log.warn("payment.failed: No RazorpayTransaction found for resourceId={}", resourceId)
        );
    }

    /**
     * Handles {@code qr_code.credited} — fired when a QR code receives payment.
     */
    private void handleQrCredited(JSONObject event) {
        JSONObject qr  = event.getJSONObject("payload").getJSONObject("qr_code").getJSONObject("entity");
        String qrId    = qr.getString("id");
        long paise     = qr.getLong("payments_amount_received");
        log.info("qr_code.credited: qrId={}, amount=Rs.{}", qrId, paise / 100.0);

        transactionRepository.findByRazorpayResourceId(qrId).ifPresentOrElse(
                txn -> {
                    if (txn.getStatus() == RazorpayTransactionStatus.CREATED) {
                        recordPaymentFromTransaction(txn, null, "Credited via QR webhook");
                    } else {
                        log.debug("qr_code.credited: transaction {} already {}, skipping", qrId, txn.getStatus());
                    }
                },
                () -> log.warn("qr_code.credited: No RazorpayTransaction found for qrId={}", qrId)
        );
    }

    /**
     * Common helper: builds a {@link PaymentCreateDto} from a {@link RazorpayTransaction}
     * and delegates to {@link PaymentService#recordPayment}.
     * Updates the transaction to PAID on success, FAILED on error.
     */
    private void recordPaymentFromTransaction(RazorpayTransaction txn, String razorpayPaymentId, String source) {
        try {
            List<PaymentAllocationDto> allocations = deserializeAllocations(txn.getAllocationsJson());
            PaymentCreateDto createDto = PaymentCreateDto.builder()
                    .tenantId(txn.getTenantId())
                    .customerId(txn.getCustomerId())
                    .totalAmount(fromPaise(txn.getAmountInPaise()))
                    .paymentMethod(PaymentMethod.RAZOR_PAY)
                    .referenceNumber(razorpayPaymentId != null ? razorpayPaymentId : txn.getRazorpayResourceId())
                    .remarks(buildRemarks(txn.getRazorpayResourceId(), razorpayPaymentId, source))
                    .allocations(allocations)
                    .build();

            CommonResponse<?> result = paymentService.recordPayment(createDto);

            txn.setRazorpayPaymentId(razorpayPaymentId);
            txn.setStatus(RazorpayTransactionStatus.PAID);
            if (result.getId() != null) {
                txn.setPaymentRecordId(Long.parseLong(result.getId()));
            }
            transactionRepository.save(txn);
            log.info("Payment recorded from transaction: resourceId={}, paymentRecordId={}", txn.getRazorpayResourceId(), result.getId());

        } catch (Exception e) {
            log.error("recordPaymentFromTransaction failed for resourceId={}: {}", txn.getRazorpayResourceId(), e.getMessage());
            txn.setErrorDescription("Auto-record failed: " + e.getMessage());
            transactionRepository.save(txn);
        }
    }

    // ── Email ──────────────────────────────────────────────────────────────────

    private void dispatchPaymentLinkEmail(String shortUrl, Long amountInPaise,
                                          Long customerId, String toEmail) {
        try {
            String amountDisplay = amountInPaise != null
                    ? String.format("Rs. %.0f", amountInPaise / 100.0)
                    : "the requested amount";

            String subject = "Payment Request from EZH Inventory";
            String body = "Hello,\n\n"
                    + "A payment of " + amountDisplay + " has been requested.\n\n"
                    + "Click the link below to complete your payment securely:\n"
                    + shortUrl + "\n\n"
                    + "This link is valid for 24 hours.\n\n"
                    + "If you have any questions, please contact us.\n\n"
                    + "Regards,\nEZH Inventory Team";

            notificationService.send(NotificationRequest.builder()
                    .type(NotificationType.INFO)
                    .targetScope(TargetType.USER)
                    .subject(subject)
                    .body(body)
                    .distributors(List.of(NotificationDistributor.builder()
                            .channel(NotificationChannel.EMAIL)
                            .toEmails(List.of(toEmail))
                            .build()))
                    .metadata(Map.of("customerId", String.valueOf(customerId), "sentBy", "razorpay-service"))
                    .build());

            log.info("Payment link email dispatched to {} for customer {}", toEmail, customerId);

        } catch (Exception e) {
            log.warn("Failed to send payment link email to {}: {}", toEmail, e.getMessage());
        }
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
            log.warn("Could not close QR code {}: {}", qrCodeId, e.getMessage());
        }
    }

    private void cancelPaymentLink(String paymentLinkId) {
        try {
            razorpayClient.paymentLink.cancel(paymentLinkId);
            log.info("Cancelled Payment Link: {}", paymentLinkId);
        } catch (RazorpayException e) {
            log.warn("Could not cancel Payment Link {}: {}", paymentLinkId, e.getMessage());
        }
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


    private long toP(BigDecimal rupees) {
        return rupees.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private BigDecimal fromPaise(long paise) {
        return BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100));
    }

    private String buildRemarks(String resourceId, String paymentId, String extra) {
        String base = "Razorpay | Order: " + resourceId
                + (paymentId != null ? " | Payment: " + paymentId : "");
        return extra != null ? base + " | " + extra : base;
    }

    private String serializeAllocations(List<PaymentAllocationDto> allocations) {
        if (allocations == null || allocations.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(allocations);
        } catch (Exception e) {
            log.warn("Failed to serialize allocations: {}", e.getMessage());
            return null;
        }
    }

    private List<PaymentAllocationDto> deserializeAllocations(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PaymentAllocationDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize allocations: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
