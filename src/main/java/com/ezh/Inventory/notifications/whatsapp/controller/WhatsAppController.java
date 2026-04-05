package com.ezh.Inventory.notifications.whatsapp.controller;

import com.ezh.Inventory.notifications.whatsapp.config.WhatsAppProperties;
import com.ezh.Inventory.notifications.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for WhatsApp operations.
 *
 * GET  /api/whatsapp/webhook  – Meta webhook verification (hub.challenge handshake)
 * POST /api/whatsapp/webhook  – Receive incoming messages / delivery receipts
 * POST /api/whatsapp/send/text      – Send a plain-text message (admin / test use)
 * POST /api/whatsapp/send/otp       – Send an OTP via template
 * POST /api/whatsapp/send/notify    – Send a titled notification as WhatsApp text
 */
@Slf4j
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppProperties props;

    // ── Meta Webhook Verification ──────────────────────────────────────────
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && token.equals(props.getWebhookVerifyToken())) {
            log.info("WhatsApp webhook verified.");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed.");
        return ResponseEntity.status(403).body("Forbidden");
    }

    // ── Incoming messages / status updates from Meta ───────────────────────
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        log.debug("WhatsApp webhook payload: {}", payload);
        // Future: parse delivery receipts, read receipts, incoming replies
        return ResponseEntity.ok().build();
    }

    // ── Outbound helpers (internal / admin use) ────────────────────────────
    @PostMapping("/send/text")
    public ResponseEntity<String> sendText(
            @RequestParam String phone,
            @RequestParam String message) {
        boolean sent = whatsAppService.sendText(phone, message);
        return sent ? ResponseEntity.ok("Sent") : ResponseEntity.internalServerError().body("Failed");
    }

    @PostMapping("/send/otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String phone,
            @RequestParam String otp) {
        boolean sent = whatsAppService.sendOtp(phone, otp);
        return sent ? ResponseEntity.ok("OTP sent") : ResponseEntity.internalServerError().body("Failed");
    }

    @PostMapping("/send/notify")
    public ResponseEntity<String> sendNotification(
            @RequestParam String phone,
            @RequestParam String title,
            @RequestParam String message) {
        boolean sent = whatsAppService.sendNotification(phone, title, message);
        return sent ? ResponseEntity.ok("Notification sent") : ResponseEntity.internalServerError().body("Failed");
    }
}
