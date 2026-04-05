package com.ezh.Inventory.notifications.gmail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gmail-specific sender settings.
 *
 * <p>SMTP transport is handled by Spring's auto-configured {@code JavaMailSender}
 * via {@code spring.mail.*} properties. This class holds the application-level
 * settings (display name, reply-to, template names, etc.).
 *
 * <pre>
 * # application.properties
 * gmail.sender-name=EZH Notifications
 * gmail.sender-email=ezh.notifications@gmail.com
 * gmail.reply-to=support@ezh.com
 * gmail.otp-validity-minutes=10
 * gmail.otp-template-name=otp
 * gmail.support-url=https://ezh.com/support
 * gmail.logo-url=https://ezh.com/assets/logo.png
 * gmail.enabled=true
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "gmail")
public class GmailProperties {

    /** Display name shown in the From header, e.g. "EZH Notifications". */
    private String senderName = "EZH Notifications";

    /**
     * Sender Gmail address. Should match {@code spring.mail.username}.
     * Used to build the "From: Name <address>" header.
     */
    private String senderEmail = "";

    /** Optional Reply-To address (e.g. "support@ezh.com"). */
    private String replyTo = "";

    /** How long OTP codes are valid — inserted into the OTP email template. */
    private int otpValidityMinutes = 10;

    /** Logical name for the OTP template (informational / future use). */
    private String otpTemplateName = "otp";

    /** Support page URL embedded in transactional email footers. */
    private String supportUrl = "https://ezh.com/support";

    /** Logo URL embedded in HTML email templates. */
    private String logoUrl = "https://ezh.com/assets/logo.png";

    /** Set to false to disable all outbound Gmail sends without removing config. */
    private boolean enabled = true;

    /**
     * Returns the fully qualified From header value, e.g.
     * {@code "EZH Notifications <ezh.notifications@gmail.com>"}.
     */
    public String fromAddress() {
        if (senderEmail == null || senderEmail.isBlank()) return senderName;
        return senderName + " <" + senderEmail + ">";
    }
}
