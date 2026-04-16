package com.ezh.Inventory.utils.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMiniDto {
    private Long id;
    private String userType;
    private String userUuid;
    private String name;
    private String email;
    private String phone;
    private List<UserAddressDto> userAddresses;
}
