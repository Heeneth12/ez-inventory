package com.ezh.Inventory.utils.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAddressDto {

    private Long id;

    @NotBlank(message = "Address Line 1 is required")
    private String addressLine1;
    private String addressLine2;
    private String route;
    private String area;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Pin Code is required")
    private String pinCode;

    @NotNull(message = "Address Type is required")
    private AddressType type;
}
