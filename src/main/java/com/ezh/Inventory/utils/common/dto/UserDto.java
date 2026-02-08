package com.ezh.Inventory.utils.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private Long tenantId;
    private String userUuid;
    private String fullName;
    private String email;
    private String phone;
    private Boolean isActive;
    private String userType;
    private Set<String> roles;
    private List<UserRoleDto> userRoles;
    private Set<Long> applicationIds;
    private Set<UserAddressDto> addresses;
    private List<UserAppEditDto> userApplications;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAppEditDto {
        private Long applicationId;
        private String appName;
        private Boolean isActive;
        private List<UserModulePrivilegeDto> modulePrivileges;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserModulePrivilegeDto {
        private Long moduleId;
        private Long privilegeId;
        private String privilegeName; // e.g., "Read Only"
        private String privilegeKey;  // e.g., "READ"
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleDto {
        private Long id;
        private String roleKey;
        private String roleName;
        private Long roleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressDto {
        private Long id;
        private Long userId;
        private String addressLine1;
        private String addressLine2;
        private String route;
        private String area;
        private String city;
        private String state;
        private String country;
        private String pinCode;
        private AddressType type;
    }

    public enum AddressType {
        BILLING,
        SHIPPING,
        OFFICE,
        HOME,
        OTHER
    }
}
