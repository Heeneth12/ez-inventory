package com.ezh.Inventory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserContext userContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            // Validate token and check if it's an access token
            if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                // Extract Long IDs
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);

                // Extract UUIDs (Assuming your JwtTokenProvider has these methods)
                String userUuid = jwtTokenProvider.getUserUuidFromToken(token);
                String tenantUuid = jwtTokenProvider.getTenantUuidFromToken(token);

                String email = jwtTokenProvider.getEmailFromToken(token);
                String userType = jwtTokenProvider.getUserTypeFromToken(token);
                String roles = jwtTokenProvider.getRolesFromToken(token);

                // Populate UserContext (ensure UserContext class is updated with these fields)
                userContext.setUserId(userId);
                userContext.setUserUuid(userUuid);
                userContext.setEmail(email);
                userContext.setTenantId(tenantId);
                userContext.setTenantUuid(tenantUuid);
                userContext.setUserType(userType);
                userContext.setRoles(roles);

                // Create authentication object with the new UUID parameters
                JwtAuthentication authentication = new JwtAuthentication(
                        userId,
                        userUuid,
                        email,
                        tenantId,
                        tenantUuid,
                        userType,
                        roles
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log the exception if needed
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}