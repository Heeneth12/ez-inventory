package com.ezh.Inventory.notifications.gmail.service;

import com.ezh.Inventory.notifications.gmail.config.GmailProperties;
import com.ezh.Inventory.notifications.gmail.dto.GmailRequest;
import com.ezh.Inventory.notifications.gmail.dto.GmailTemplateRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends emails via Gmail using Spring's {@link JavaMailSender}.
 *
 * <p>Mirrors {@code WhatsAppService} in structure and responsibility.
 *
 * <p>Prerequisites in {@code application.properties}:
 * <pre>
 * # SMTP transport (Spring auto-configuration)
 * spring.mail.host=smtp.gmail.com
 * spring.mail.port=587
 * spring.mail.username=your-account@gmail.com
 * spring.mail.password=your-app-password        # Gmail App Password, NOT account password
 * spring.mail.properties.mail.smtp.auth=true
 * spring.mail.properties.mail.smtp.starttls.enable=true
 *
 * # Application-level sender settings
 * gmail.sender-name=EZH Notifications
 * gmail.sender-email=your-account@gmail.com
 * gmail.reply-to=support@ezh.com
 * gmail.otp-validity-minutes=10
 * gmail.support-url=https://ezh.com/support
 * gmail.logo-url=https://ezh.com/assets/logo.png
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private final GmailProperties props;
    private final JavaMailSender mailSender;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Send a plain-text email.
     *
     * @param toEmail recipient address
     * @param subject email subject
     * @param body    plain-text body
     * @return true if the mail was accepted by the SMTP server
     */
    public boolean sendText(String toEmail, String subject, String body) {
        if (!isConfigured()) return false;
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(props.fromAddress());
            mail.setTo(toEmail);
            mail.setSubject(subject);
            mail.setText(body);
            if (!props.getReplyTo().isBlank()) mail.setReplyTo(props.getReplyTo());
            mailSender.send(mail);
            log.info("Plain email sent | to={} | subject='{}'", toEmail, subject);
            return true;
        } catch (Exception e) {
            log.error("Plain email failed | to={} | error={}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send an HTML email using a {@link MimeMessage}.
     *
     * @param toEmail   recipient address
     * @param subject   email subject
     * @param htmlBody  fully formed HTML string
     * @return true if accepted
     */
    public boolean sendHtml(String toEmail, String subject, String htmlBody) {
        if (!isConfigured()) return false;
        return sendMime(GmailRequest.builder()
                .to(toEmail)
                .subject(subject)
                .body(htmlBody)
                .html(true)
                .build());
    }

    /**
     * Send a fully configured email from a {@link GmailRequest}.
     * Supports plain text, HTML, CC, BCC, and custom From / Reply-To.
     *
     * @param request populated {@link GmailRequest}
     * @return true if accepted
     */
    public boolean send(GmailRequest request) {
        if (!isConfigured()) return false;
        return request.isHtml() ? sendMime(request) : sendSimple(request);
    }

    /**
     * Send an OTP email using the built-in OTP HTML template.
     *
     * @param toEmail   recipient address
     * @param otp       OTP code to embed
     * @param name      recipient's display name (optional, nullable)
     * @return true if accepted
     */
    public boolean sendOtp(String toEmail, String otp, String name) {
        if (!isConfigured()) return false;
        String html = buildOtpTemplate(otp, name != null ? name : "");
        log.info("OTP email dispatched | to={}", toEmail);
        return sendHtml(toEmail, "Your One-Time Password", html);
    }

    /**
     * Mirror an in-app notification to email (plain-text).
     * Equivalent to {@code WhatsAppService.sendNotification()}.
     *
     * @param toEmail   recipient address
     * @param subject   notification subject / title
     * @param body      notification body
     * @return true if accepted
     */
    public boolean sendNotification(String toEmail, String subject, String body) {
        if (!isConfigured()) return false;
        String html = buildNotificationTemplate(subject, body);
        return sendHtml(toEmail, subject, html);
    }

    /**
     * Send a welcome / registration email using the built-in welcome template.
     *
     * @param toEmail  recipient address
     * @param name     recipient's full name
     * @return true if accepted
     */
    public boolean sendWelcome(String toEmail, String name) {
        if (!isConfigured()) return false;
        String html = buildWelcomeTemplate(name);
        return sendHtml(toEmail, "Welcome to EZH — you're all set!", html);
    }

    /**
     * Send a password-reset link email.
     *
     * @param toEmail    recipient address
     * @param name       recipient's display name
     * @param resetLink  full URL of the reset page
     * @return true if accepted
     */
    public boolean sendPasswordReset(String toEmail, String name, String resetLink) {
        if (!isConfigured()) return false;
        String html = buildPasswordResetTemplate(name, resetLink);
        return sendHtml(toEmail, "Reset your EZH password", html);
    }

    /**
     * Send an order status / shipment notification email.
     *
     * @param toEmail  recipient address
     * @param name     recipient's display name
     * @param orderId  order identifier
     * @param status   order status label (e.g. "Shipped", "Delivered")
     * @param details  optional additional details (nullable)
     * @return true if accepted
     */
    public boolean sendOrderNotification(String toEmail, String name,
                                          String orderId, String status, String details) {
        if (!isConfigured()) return false;
        String html = buildOrderTemplate(name, orderId, status, details != null ? details : "");
        return sendHtml(toEmail, "Order #" + orderId + " — " + status, html);
    }

    /**
     * Send a template-based email from a {@link GmailTemplateRequest}.
     * Variables in the request are substituted into the matching built-in template.
     *
     * @param request template request
     * @return true if accepted
     */
    public boolean sendTemplate(GmailTemplateRequest request) {
        if (!isConfigured()) return false;

        Map<String, String> v = request.getVariables();

        String html = switch (request.getTemplate()) {
            case OTP -> buildOtpTemplate(
                    v.getOrDefault("otp", ""),
                    v.getOrDefault("name", ""));
            case WELCOME -> buildWelcomeTemplate(
                    v.getOrDefault("name", ""));
            case PASSWORD_RESET -> buildPasswordResetTemplate(
                    v.getOrDefault("name", ""),
                    v.getOrDefault("resetLink", "#"));
            case ORDER_NOTIFICATION -> buildOrderTemplate(
                    v.getOrDefault("name", ""),
                    v.getOrDefault("orderId", ""),
                    v.getOrDefault("status", ""),
                    v.getOrDefault("details", ""));
            case GENERAL_NOTIFICATION -> buildNotificationTemplate(
                    v.getOrDefault("subject", "Notification"),
                    v.getOrDefault("body", ""));
        };

        String subject = resolveSubject(request.getTemplate(), v);
        GmailRequest mail = GmailRequest.builder()
                .to(request.getTo())
                .from(request.getFrom())
                .replyTo(request.getReplyTo())
                .subject(subject)
                .body(html)
                .html(true)
                .build();

        log.info("Template email dispatched | template={} | to={}", request.getTemplate(), request.getTo());
        return sendMime(mail);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal send helpers
    // ──────────────────────────────────────────────────────────────────────────

    private boolean sendSimple(GmailRequest req) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(req.getFrom() != null ? req.getFrom() : props.fromAddress());
            mail.setTo(req.getTo());
            mail.setSubject(req.getSubject());
            mail.setText(req.getBody());
            if (req.getReplyTo() != null) mail.setReplyTo(req.getReplyTo());
            if (!req.getCcs().isEmpty())  mail.setCc(req.getCcs().toArray(new String[0]));
            if (!req.getBccs().isEmpty()) mail.setBcc(req.getBccs().toArray(new String[0]));
            mailSender.send(mail);
            log.info("Email sent | to={} | subject='{}'", req.getTo(), req.getSubject());
            return true;
        } catch (Exception e) {
            log.error("Email failed | to={} | error={}", req.getTo(), e.getMessage(), e);
            return false;
        }
    }

    private boolean sendMime(GmailRequest req) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(req.getFrom() != null ? req.getFrom() : props.fromAddress());
            helper.setTo(req.getTo());
            helper.setSubject(req.getSubject());
            helper.setText(req.getBody(), req.isHtml());
            if (req.getReplyTo() != null) helper.setReplyTo(req.getReplyTo());
            if (!req.getCcs().isEmpty())  helper.setCc(req.getCcs().toArray(new String[0]));
            if (!req.getBccs().isEmpty()) helper.setBcc(req.getBccs().toArray(new String[0]));
            mailSender.send(mime);
            log.info("HTML email sent | to={} | subject='{}'", req.getTo(), req.getSubject());
            return true;
        } catch (Exception e) {
            log.error("HTML email failed | to={} | error={}", req.getTo(), e.getMessage(), e);
            return false;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HTML Templates
    // ──────────────────────────────────────────────────────────────────────────

    private String buildOtpTemplate(String otp, String name) {
        String greeting = (name != null && !name.isBlank())
                ? "Hi <strong>" + name + "</strong>,"
                : "Hi,";
        return wrapLayout(
            "<p style=\"margin:0 0 16px;color:#374151;font-size:15px;\">" + greeting + "</p>" +
            "<p style=\"margin:0 0 24px;color:#374151;font-size:15px;\">" +
                "Use the One-Time Password below to complete your sign-in." +
            "</p>" +
            "<div style=\"background:#f3f4f6;border:2px dashed #4f46e5;border-radius:12px;" +
                    "padding:24px;text-align:center;margin:0 0 24px;\">" +
                "<span style=\"font-size:38px;font-weight:700;color:#4f46e5;" +
                        "letter-spacing:12px;font-family:monospace;\">" + otp + "</span>" +
            "</div>" +
            "<p style=\"margin:0 0 8px;color:#6b7280;font-size:13px;\">" +
                "This code expires in <strong>" + props.getOtpValidityMinutes() + " minutes</strong>." +
            "</p>" +
            "<p style=\"margin:0;color:#9ca3af;font-size:12px;\">" +
                "If you didn't request this, you can safely ignore this email." +
            "</p>",
            "Your One-Time Password"
        );
    }

    private String buildWelcomeTemplate(String name) {
        return wrapLayout(
            "<p style=\"margin:0 0 16px;color:#374151;font-size:15px;\">" +
                "Hi <strong>" + name + "</strong>, welcome aboard! \uD83C\uDF89" +
            "</p>" +
            "<p style=\"margin:0 0 24px;color:#374151;font-size:15px;\">" +
                "Your EZH account has been successfully created. " +
                "You can now log in and start managing your inventory." +
            "</p>" +
            "<div style=\"text-align:center;margin:0 0 24px;\">" +
                "<a href=\"" + props.getSupportUrl() + "\" " +
                    "style=\"background:#4f46e5;color:#fff;text-decoration:none;" +
                    "padding:12px 28px;border-radius:8px;font-size:14px;font-weight:600;" +
                    "display:inline-block;\">Get Started</a>" +
            "</div>" +
            "<p style=\"margin:0;color:#9ca3af;font-size:12px;\">" +
                "Need help? Visit our <a href=\"" + props.getSupportUrl() + "\" " +
                "style=\"color:#4f46e5;\">support centre</a>." +
            "</p>",
            "Welcome to EZH"
        );
    }

    private String buildPasswordResetTemplate(String name, String resetLink) {
        return wrapLayout(
            "<p style=\"margin:0 0 16px;color:#374151;font-size:15px;\">" +
                "Hi <strong>" + name + "</strong>," +
            "</p>" +
            "<p style=\"margin:0 0 24px;color:#374151;font-size:15px;\">" +
                "We received a request to reset your EZH account password. " +
                "Click the button below — this link is valid for <strong>30 minutes</strong>." +
            "</p>" +
            "<div style=\"text-align:center;margin:0 0 24px;\">" +
                "<a href=\"" + resetLink + "\" " +
                    "style=\"background:#ef4444;color:#fff;text-decoration:none;" +
                    "padding:12px 28px;border-radius:8px;font-size:14px;font-weight:600;" +
                    "display:inline-block;\">Reset Password</a>" +
            "</div>" +
            "<p style=\"margin:0 0 8px;color:#9ca3af;font-size:12px;\">" +
                "Or copy and paste this link into your browser:" +
            "</p>" +
            "<p style=\"margin:0 0 16px;color:#4f46e5;font-size:12px;word-break:break-all;\">" +
                resetLink +
            "</p>" +
            "<p style=\"margin:0;color:#9ca3af;font-size:12px;\">" +
                "If you didn't request a password reset, please ignore this email or " +
                "<a href=\"" + props.getSupportUrl() + "\" style=\"color:#4f46e5;\">contact support</a>." +
            "</p>",
            "Reset Your Password"
        );
    }

    private String buildOrderTemplate(String name, String orderId, String status, String details) {
        String badgeColor = switch (status.toLowerCase()) {
            case "shipped"   -> "#3b82f6";
            case "delivered" -> "#10b981";
            case "cancelled" -> "#ef4444";
            case "processing"-> "#f59e0b";
            default          -> "#6b7280";
        };

        return wrapLayout(
            "<p style=\"margin:0 0 16px;color:#374151;font-size:15px;\">" +
                "Hi <strong>" + name + "</strong>," +
            "</p>" +
            "<p style=\"margin:0 0 20px;color:#374151;font-size:15px;\">" +
                "There's an update on your order." +
            "</p>" +
            "<div style=\"background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;" +
                    "padding:20px;margin:0 0 24px;\">" +
                "<div style=\"display:flex;justify-content:space-between;align-items:center;\">" +
                    "<span style=\"font-size:13px;color:#6b7280;\">Order ID</span>" +
                    "<strong style=\"font-size:14px;color:#111827;\">#" + orderId + "</strong>" +
                "</div>" +
                "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:12px 0;\"/>" +
                "<div style=\"display:flex;justify-content:space-between;align-items:center;\">" +
                    "<span style=\"font-size:13px;color:#6b7280;\">Status</span>" +
                    "<span style=\"background:" + badgeColor + ";color:#fff;" +
                            "padding:3px 10px;border-radius:999px;font-size:12px;" +
                            "font-weight:600;\">" + status + "</span>" +
                "</div>" +
                (details.isBlank() ? "" :
                    "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:12px 0;\"/>" +
                    "<p style=\"margin:0;font-size:13px;color:#374151;\">" + details + "</p>"
                ) +
            "</div>" +
            "<p style=\"margin:0;color:#9ca3af;font-size:12px;\">" +
                "Questions? <a href=\"" + props.getSupportUrl() + "\" style=\"color:#4f46e5;\">" +
                "Contact support</a>." +
            "</p>",
            "Order Update"
        );
    }

    private String buildNotificationTemplate(String subject, String body) {
        return wrapLayout(
            "<p style=\"margin:0 0 16px;color:#374151;font-size:15px;font-weight:600;\">" +
                subject +
            "</p>" +
            "<p style=\"margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;\">" +
                body +
            "</p>" +
            "<p style=\"margin:0;color:#9ca3af;font-size:12px;\">" +
                "This is an automated notification from EZH. " +
                "<a href=\"" + props.getSupportUrl() + "\" style=\"color:#4f46e5;\">Support</a>" +
            "</p>",
            subject
        );
    }

    /**
     * Wraps content in the shared EZH email layout (header bar + footer).
     */
    private String wrapLayout(String content, String previewText) {
        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
                "<meta charset=\"UTF-8\"/>" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>" +
                "<title>" + previewText + "</title>" +
                "<!--[if mso]><style>body,table,td,p{font-family:Arial,sans-serif!important}</style><![endif]-->" +
            "</head>" +
            "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"" +
                        " style=\"background:#f1f5f9;padding:40px 16px;\">" +
                    "<tr><td align=\"center\">" +
                        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"" +
                                " style=\"max-width:560px;\">" +

                            // ── Header ──
                            "<tr><td style=\"background:#4f46e5;border-radius:12px 12px 0 0;" +
                                    "padding:24px 32px;text-align:center;\">" +
                                "<span style=\"color:#fff;font-size:22px;font-weight:700;" +
                                        "letter-spacing:-0.5px;\">EZH</span>" +
                            "</td></tr>" +

                            // ── Body ──
                            "<tr><td style=\"background:#ffffff;padding:32px;\">" +
                                content +
                            "</td></tr>" +

                            // ── Footer ──
                            "<tr><td style=\"background:#f9fafb;border-radius:0 0 12px 12px;" +
                                    "padding:16px 32px;text-align:center;\">" +
                                "<p style=\"margin:0;font-size:11px;color:#9ca3af;\">" +
                                    "&copy; 2025 EZH Inventory Management. All rights reserved.<br/>" +
                                    "<a href=\"" + props.getSupportUrl() + "\" " +
                                    "style=\"color:#6b7280;text-decoration:underline;\">Support</a>" +
                                "</p>" +
                            "</td></tr>" +

                        "</table>" +
                    "</td></tr>" +
                "</table>" +
            "</body>" +
            "</html>";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internals
    // ──────────────────────────────────────────────────────────────────────────

    private String resolveSubject(GmailTemplateRequest.TemplateName template,
                                   Map<String, String> v) {
        return switch (template) {
            case OTP                 -> "Your One-Time Password";
            case WELCOME             -> "Welcome to EZH — you're all set!";
            case PASSWORD_RESET      -> "Reset your EZH password";
            case ORDER_NOTIFICATION  -> "Order #" + v.getOrDefault("orderId", "") +
                                        " — " + v.getOrDefault("status", "Update");
            case GENERAL_NOTIFICATION -> v.getOrDefault("subject", "Notification from EZH");
        };
    }

    private boolean isConfigured() {
        if (!props.isEnabled()) {
            log.debug("Gmail is disabled — gmail.enabled=false");
            return false;
        }
        return true;
    }
}
