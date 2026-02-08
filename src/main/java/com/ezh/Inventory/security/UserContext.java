package com.ezh.Inventory.security;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@Component
@RequestScope
public class UserContext {
    private Long userId;
    private String email;
    private Long tenantId;
    private String userType;
    private String roles;
}
