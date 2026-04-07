package com.ezh.Inventory.notifications.gmail.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Payload for a plain-text or HTML email sent via Gmail / JavaMailSender.
 *
 * <p>Mirrors the role of {@code WhatsAppTextRequest} in the WhatsApp module.
 *
 * <pre>
 * GmailRequest.builder()
 *     .to("customer@example.com")
 *     .subject("Your order is ready")
 *     .body("Order #4521 has been dispatched.")
 *     .html(false)
 *     .build();
 * </pre>
 */
@Data
@Builder
public class GmailRequest {

    /** Recipient email address. */
    private String to;

    /**
     * Sender address override.
     * Leave null to use the default from {@code GmailProperties.fromAddress()}.
     */
    private String from;

    /** Optional Reply-To address. */
    private String replyTo;

    /** Email subject line. */
    private String subject;

    /** Email body — plain text or HTML depending on the {@code html} flag. */
    private String body;

    /**
     * When {@code true}, {@code body} is treated as HTML and sent as a
     * {@code MimeMessage}. When {@code false}, a simple {@code SimpleMailMessage}
     * is used.
     */
    @Builder.Default
    private boolean html = false;

    /** Optional CC recipients. */
    @Singular
    private List<String> ccs;

    /** Optional BCC recipients. */
    @Singular
    private List<String> bccs;
}
