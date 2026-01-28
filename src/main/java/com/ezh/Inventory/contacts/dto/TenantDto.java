package com.ezh.Inventory.contacts.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {
    private Long id;
    private String tenantUuid;
    private String tenantName;
    private String tenantCode;
    private String email;
    private String phone;
    private Boolean isActive;
}
