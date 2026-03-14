package com.ezh.Inventory.utils.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailsDto {

    @NotNull(message = "Business type is required")
    private BusinessType businessType;

    @NotBlank(message = "Legal name is required")
    private String legalName;

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3)
    private String baseCurrency;

    @NotBlank(message = "Timezone is required")
    private String timeZone;

    private String gstNumber;
    private String panNumber;

    @Email(message = "Invalid email format")
    private String supportEmail;

    private String contactPhone;
    private String logoUrl;
    private String website;
}
