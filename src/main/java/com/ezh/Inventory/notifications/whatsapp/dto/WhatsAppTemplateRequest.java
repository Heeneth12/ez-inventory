package com.ezh.Inventory.notifications.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Payload for a WhatsApp template message (e.g. OTP) via Meta Cloud API.
 */
@Data
@Builder
public class WhatsAppTemplateRequest {

    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    private String to;

    @Builder.Default
    private String type = "template";

    private Template template;

    // ──────────────────────────────────────────────────────────────────
    @Data
    @Builder
    public static class Template {
        private String name;
        private Language language;

        @Singular
        private List<Component> components;
    }

    @Data
    @Builder
    public static class Language {
        private String code;
    }

    @Data
    @Builder
    public static class Component {
        private String type;       // "body" | "button"

        @JsonProperty("sub_type")
        private String subType;    // "url" (for copy-code button)

        private String index;      // button index as string

        @Singular
        private List<Parameter> parameters;
    }

    @Data
    @Builder
    public static class Parameter {
        private String type;       // "text"
        private String text;
    }
}
