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

    // Standard Getters

    public static Long getUserId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getUserId() : null;
    }

    public static Long getTenantId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getTenantId() : null;
    }

    /**
     * Retrieves the User UUID from the security context.
     */
    public static String getUserUuid() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getUserUuid() : null;
    }

    /**
     * Retrieves the Tenant UUID from the security context.
     */
    public static String getTenantUuid() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getTenantUuid() : null;
    }

    public static String getEmail() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getEmail() : null;
    }

    // "Or Throw" Methods for UUIDs

    public static String getUserUuidOrThrow() throws CommonException {
        String userUuid = getUserUuid();
        if (userUuid == null) {
            throw new CommonException("User UUID missing in request", HttpStatus.UNAUTHORIZED);
        }
        return userUuid;
    }

    public static String getTenantUuidOrThrow() throws CommonException {
        String tenantUuid = getTenantUuid();
        if (tenantUuid == null) {
            throw new CommonException("Tenant UUID missing in request", HttpStatus.UNAUTHORIZED);
        }
        return tenantUuid;
    }

    // Existing "Or Throw" Methods

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