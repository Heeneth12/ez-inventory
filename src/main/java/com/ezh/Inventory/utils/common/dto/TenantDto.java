package com.ezh.Inventory.utils.common.dto;


import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private Long id;
    private String tenantUuid;
    private String tenantName;
    private String tenantCode;
    private String email;
    private String phone;
    private Boolean isActive;
    private UserDto tenantAdmin;
    private Set<ApplicationDto> applications;
    private Set<TenantAddressDto> tenantAddress;
    private TenantDetailsDto tenantDetails;
}
