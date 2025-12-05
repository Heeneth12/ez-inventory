package com.ezh.Inventory.utils;

import com.ezh.Inventory.security.JwtAuthentication;
import com.ezh.Inventory.utils.exception.CommonException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextUtil {

    private UserContextUtil() {
        // private constructor to prevent object creation
    }

    private static JwtAuthentication getAuth() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthentication auth) {
            return auth;
        }
        return null;
    }

    public static Long getUserId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getUserId() : null;
    }

    public static Long getTenantId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getTenantId() : null;
    }

    public static String getEmail() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getEmail() : null;
    }


    public static Long getTenantIdOrThrow() throws CommonException {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new CommonException("Tenant id missing in request", HttpStatus.UNAUTHORIZED);
        }
        return tenantId;
    }

    public static Long getUserIdOrThrow() throws CommonException {
        Long userId = getUserId();
        if (userId == null) {
            throw new CommonException("User id missing in request", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
