package com.ezh.Inventory.notifications.gmail.controller;

import com.ezh.Inventory.notifications.gmail.dto.GmailRequest;
import com.ezh.Inventory.notifications.gmail.dto.GmailTemplateRequest;
import com.ezh.Inventory.notifications.gmail.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for Gmail / Email operations.
 *
 * <pre>
 * POST /api/gmail/send/text            – Send a plain-text email
 * POST /api/gmail/send/html            – Send an HTML email
 * POST /api/gmail/send/notify          – Mirror a notification to email
 * POST /api/gmail/send/otp             – Send an OTP email
 * POST /api/gmail/send/welcome         – Send a welcome email
 * POST /api/gmail/send/password-reset  – Send a password-reset link email
 * POST /api/gmail/send/order           – Send an order status email
 * POST /api/gmail/send/template        – Send any template by name
 * </pre>
 *
 * <p>Mirrors {@code WhatsAppController} in structure and conventions.
 */
@Slf4j
@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailService gmailService;

    // ── Plain text ─────────────────────────────────────────────────────────────

    /**
     * Send a plain-text email.
     *
     * <pre>
     * POST /api/gmail/send/text?to=user@example.com&subject=Hello&body=World
     * </pre>
     */
    @PostMapping("/send/text")
    public ResponseEntity<String> sendText(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {

        boolean sent = gmailService.sendText(to, subject, body);
        return sent
                ? ResponseEntity.ok("Email sent")
                : ResponseEntity.internalServerError().body("Failed to send email");
    }

    // ── HTML ───────────────────────────────────────────────────────────────────

    /**
     * Send an HTML email. Provide raw HTML in the request body.
     *
     * <pre>
     * POST /api/gmail/send/html?to=user@example.com&subject=Hello
     * Body: &lt;h1&gt;Hello World&lt;/h1&gt;
     * </pre>
     */
    @PostMapping("/send/html")
    public ResponseEntity<String> sendHtml(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestBody String htmlBody) {

        boolean sent = gmailService.sendHtml(to, subject, htmlBody);
        return sent
                ? ResponseEntity.ok("HTML email sent")
                : ResponseEntity.internalServerError().body("Failed to send HTML email");
    }

    // ── Notification (mirror of WhatsApp notify endpoint) ─────────────────────

    /**
     * Mirror a notification as a styled HTML email.
     *
     * <pre>
     * POST /api/gmail/send/notify?to=user@example.com&subject=Alert&body=Your+stock+is+low
     * </pre>
     */
    @PostMapping("/send/notify")
    public ResponseEntity<String> sendNotification(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {

        boolean sent = gmailService.sendNotification(to, subject, body);
        return sent
                ? ResponseEntity.ok("Notification email sent")
                : ResponseEntity.internalServerError().body("Failed to send notification email");
    }

    // ── OTP ───────────────────────────────────────────────────────────────────

    /**
     * Send a One-Time Password email.
     *
     * <pre>
     * POST /api/gmail/send/otp?to=user@example.com&otp=847291&name=John
     * </pre>
     */
    @PostMapping("/send/otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String to,
            @RequestParam String otp,
            @RequestParam(required = false) String name) {

        boolean sent = gmailService.sendOtp(to, otp, name);
        return sent
                ? ResponseEntity.ok("OTP email sent")
                : ResponseEntity.internalServerError().body("Failed to send OTP email");
    }

    // ── Welcome ───────────────────────────────────────────────────────────────

    /**
     * Send a welcome / registration email.
     *
     * <pre>
     * POST /api/gmail/send/welcome?to=user@example.com&name=John+Doe
     * </pre>
     */
    @PostMapping("/send/welcome")
    public ResponseEntity<String> sendWelcome(
            @RequestParam String to,
            @RequestParam String name) {

        boolean sent = gmailService.sendWelcome(to, name);
        return sent
                ? ResponseEntity.ok("Welcome email sent")
                : ResponseEntity.internalServerError().body("Failed to send welcome email");
    }

    // ── Password reset ─────────────────────────────────────────────────────────

    /**
     * Send a password-reset link email.
     *
     * <pre>
     * POST /api/gmail/send/password-reset?to=user@example.com&name=John&resetLink=https://...
     * </pre>
     */
    @PostMapping("/send/password-reset")
    public ResponseEntity<String> sendPasswordReset(
            @RequestParam String to,
            @RequestParam String name,
            @RequestParam String resetLink) {

        boolean sent = gmailService.sendPasswordReset(to, name, resetLink);
        return sent
                ? ResponseEntity.ok("Password-reset email sent")
                : ResponseEntity.internalServerError().body("Failed to send password-reset email");
    }

    // ── Order notification ────────────────────────────────────────────────────

    /**
     * Send an order status update email.
     *
     * <pre>
     * POST /api/gmail/send/order?to=user@example.com&name=John&orderId=4521&status=Shipped&details=ETA+3+days
     * </pre>
     */
    @PostMapping("/send/order")
    public ResponseEntity<String> sendOrderNotification(
            @RequestParam String to,
            @RequestParam String name,
            @RequestParam String orderId,
            @RequestParam String status,
            @RequestParam(required = false) String details) {

        boolean sent = gmailService.sendOrderNotification(to, name, orderId, status, details);
        return sent
                ? ResponseEntity.ok("Order email sent")
                : ResponseEntity.internalServerError().body("Failed to send order email");
    }

    // ── Template (generic) ─────────────────────────────────────────────────────

    /**
     * Send any template-based email via a JSON body.
     *
     * <pre>
     * POST /api/gmail/send/template
     * {
     *   "to": "user@example.com",
     *   "template": "OTP",
     *   "variables": { "otp": "847291", "name": "John" }
     * }
     * </pre>
     *
     * Available templates: {@code OTP}, {@code WELCOME}, {@code PASSWORD_RESET},
     * {@code ORDER_NOTIFICATION}, {@code GENERAL_NOTIFICATION}.
     */
    @PostMapping("/send/template")
    public ResponseEntity<String> sendTemplate(@RequestBody GmailTemplateRequest request) {
        boolean sent = gmailService.sendTemplate(request);
        return sent
                ? ResponseEntity.ok("Template email sent")
                : ResponseEntity.internalServerError().body("Failed to send template email");
    }

    // ── Full control ───────────────────────────────────────────────────────────

    /**
     * Full control send — supply a complete {@link GmailRequest} JSON body.
     * Supports CC, BCC, custom From, Reply-To, and HTML/plain-text toggle.
     *
     * <pre>
     * POST /api/gmail/send
     * {
     *   "to": "user@example.com",
     *   "subject": "Important update",
     *   "body": "&lt;b&gt;Hello&lt;/b&gt;",
     *   "html": true,
     *   "ccs": ["manager@company.com"],
     *   "replyTo": "support@ezh.com"
     * }
     * </pre>
     */
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody GmailRequest request) {
        boolean sent = gmailService.send(request);
        return sent
                ? ResponseEntity.ok("Email sent")
                : ResponseEntity.internalServerError().body("Failed to send email");
    }
}
