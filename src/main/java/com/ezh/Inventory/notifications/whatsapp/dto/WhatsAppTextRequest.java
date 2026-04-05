package com.ezh.Inventory.notifications.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Payload for a plain-text WhatsApp message via Meta Cloud API.
 */
@Data
@Builder
public class WhatsAppTextRequest {

    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";

    private String to;

    @Builder.Default
    private String type = "text";

    private TextBody text;

    @Data
    @Builder
    public static class TextBody {
        @JsonProperty("preview_url")
        @Builder.Default
        private boolean previewUrl = false;
        private String body;
    }
}
