package com.ezh.Inventory.notifications.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Payload for a template-based email sent via Gmail.
 *
 * <p>Mirrors the role of {@code WhatsAppTemplateRequest} in the WhatsApp module.
 * The {@link TemplateName} enum selects the pre-built HTML template; the
 * {@code variables} map provides dynamic substitution values.
 *
 * <pre>
 * // OTP email
 * GmailTemplateRequest.builder()
 *     .to("user@example.com")
 *     .template(GmailTemplateRequest.TemplateName.OTP)
 *     .variable("otp", "847291")
 *     .variable("name", "John")
 *     .build();
 *
 * // Welcome email
 * GmailTemplateRequest.builder()
 *     .to("user@example.com")
 *     .template(GmailTemplateRequest.TemplateName.WELCOME)
 *     .variable("name", "John Doe")
 *     .build();
 * </pre>
 */
@Data
@Builder
public class GmailTemplateRequest {

    /** Recipient email address. */
    private String to;

    /**
     * Sender address override.
     * Leave null to use the default from {@code GmailProperties.fromAddress()}.
     */
    private String from;

    /** Optional Reply-To address. */
    private String replyTo;

    /** Which pre-built HTML template to use. */
    private TemplateName template;

    /**
     * Dynamic substitution variables for the template.
     * Key = placeholder name (without braces), Value = replacement text.
     * Example: {@code "otp" → "847291"}, {@code "name" → "John"}.
     */
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();

    // ── Available templates ───────────────────────────────────────────────────

    public enum TemplateName {

        /**
         * One-Time Password email.
         * Required variables: {@code otp}, {@code name} (optional).
         */
        OTP,

        /**
         * Account welcome / registration email.
         * Required variables: {@code name}.
         */
        WELCOME,

        /**
         * Password-reset link email.
         * Required variables: {@code name}, {@code resetLink}.
         */
        PASSWORD_RESET,

        /**
         * Order status / shipment notification.
         * Required variables: {@code name}, {@code orderId}, {@code status},
         * {@code details} (optional).
         */
        ORDER_NOTIFICATION,

        /**
         * Generic in-app notification mirrored to email.
         * Required variables: {@code subject}, {@code body}.
         */
        GENERAL_NOTIFICATION
    }
}
