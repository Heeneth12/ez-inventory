package com.ezh.Inventory.utils.common.dto;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationDto {

    private Long id;
    private String integrationUuid;
    private Long tenantId;
    private IntegrationType integrationType;
    private String displayName;
    private String primaryKey;    // always "****" — never expose raw value
    private String secondaryKey;  // always "****" — never expose raw value
    private String tertiaryKey;   // always "****" — never expose raw value
    private Boolean isTestMode;
    private Boolean isConnected;
    private Boolean isActive;
    private String webhookConfig;
    private String extraConfig;
    private String links;
    private LocalDateTime connectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
