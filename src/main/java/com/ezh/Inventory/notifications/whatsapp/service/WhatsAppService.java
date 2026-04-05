package com.ezh.Inventory.notifications.whatsapp.service;

import com.ezh.Inventory.notifications.whatsapp.config.WhatsAppProperties;
import com.ezh.Inventory.notifications.whatsapp.dto.WhatsAppTemplateRequest;
import com.ezh.Inventory.notifications.whatsapp.dto.WhatsAppTextRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Sends WhatsApp messages via Meta Cloud API (Graph API).
 *
 * Prerequisites in Meta Developer Console:
 *   1. Create a WhatsApp Business App
 *   2. Get a permanent access token and phone-number-id
 *   3. Register OTP template named in meta.whatsapp.otp-template-name
 *
 * All config comes from application.properties under meta.whatsapp.*
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppProperties props;
    private final RestTemplate restTemplate;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Send a plain-text WhatsApp message.
     *
     * @param toPhone E.164 phone number, e.g. "919876543210"
     * @param message Text body
     * @return true if Meta accepted the request (2xx)
     */
    public boolean sendText(String toPhone, String message) {
        if (!isConfigured()) return false;

        WhatsAppTextRequest payload = WhatsAppTextRequest.builder()
                .to(sanitizePhone(toPhone))
                .text(WhatsAppTextRequest.TextBody.builder().body(message).build())
                .build();

        return post(payload);
    }

    /**
     * Send a login OTP via a pre-approved WhatsApp template.
     * The template must have one body parameter (the OTP code).
     *
     * @param toPhone E.164 phone number
     * @param otp     6-digit OTP code
     * @return true if accepted
     */
    public boolean sendOtp(String toPhone, String otp) {
        if (!isConfigured()) return false;

        WhatsAppTemplateRequest payload = WhatsAppTemplateRequest.builder()
                .to(sanitizePhone(toPhone))
                .template(WhatsAppTemplateRequest.Template.builder()
                        .name(props.getOtpTemplateName())
                        .language(WhatsAppTemplateRequest.Language.builder()
                                .code(props.getTemplateLanguage())
                                .build())
                        .components(List.of(
                                // Body parameter → OTP value
                                WhatsAppTemplateRequest.Component.builder()
                                        .type("body")
                                        .parameter(WhatsAppTemplateRequest.Parameter.builder()
                                                .type("text").text(otp).build())
                                        .build(),
                                // Copy-code / URL button parameter (index 0)
                                WhatsAppTemplateRequest.Component.builder()
                                        .type("button")
                                        .subType("url")
                                        .index("0")
                                        .parameter(WhatsAppTemplateRequest.Parameter.builder()
                                                .type("text").text(otp).build())
                                        .build()
                        ))
                        .build())
                .build();

        return post(payload);
    }

    /**
     * Send a general info / alert notification as plain text.
     * Useful to mirror in-app notifications to WhatsApp.
     *
     * @param toPhone E.164 phone number
     * @param title   Notification title
     * @param body    Notification body
     * @return true if accepted
     */
    public boolean sendNotification(String toPhone, String title, String body) {
        String text = "*" + title + "*\n" + body;
        return sendText(toPhone, text);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internals
    // ──────────────────────────────────────────────────────────────────────────

    private boolean post(Object payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getAccessToken());

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    props.messagesUrl(), request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp message sent. Status: {}", response.getStatusCode());
                return true;
            } else {
                log.warn("WhatsApp non-2xx response: {} – {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("WhatsApp send failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /** Strip leading '+' so Meta always gets a plain E.164 number. */
    private String sanitizePhone(String phone) {
        return phone != null ? phone.replaceAll("^\\+", "") : phone;
    }

    private boolean isConfigured() {
        if (props.getPhoneNumberId() == null || props.getPhoneNumberId().isBlank()) {
            log.debug("WhatsApp not configured – meta.whatsapp.phone-number-id is missing");
            return false;
        }
        if (props.getAccessToken() == null || props.getAccessToken().isBlank()) {
            log.debug("WhatsApp not configured – meta.whatsapp.access-token is missing");
            return false;
        }
        return true;
    }
}
