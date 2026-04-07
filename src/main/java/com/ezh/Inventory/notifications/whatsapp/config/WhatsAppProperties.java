package com.ezh.Inventory.notifications.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "meta.whatsapp")
public class WhatsAppProperties {

    /** Meta Graph API version, e.g. v19.0 */
    private String apiVersion = "v19.0";

    /** Your WhatsApp Business phone-number-id from Meta Developer Console */
    private String phoneNumberId;

    /** Permanent / long-lived access token from Meta Developer Console */
    private String accessToken;

    /** Template name registered in Meta Business Manager for OTP messages */
    private String otpTemplateName = "login_otp";

    /** Template language code */
    private String templateLanguage = "en";

    /** Webhook verify token – set the same value in Meta Developer Console */
    private String webhookVerifyToken;

    public String messagesUrl() {
        return "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
    }
}
