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
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);
                String userType = jwtTokenProvider.getUserTypeFromToken(token);
                String roles = jwtTokenProvider.getRolesFromToken(token);

                // Populate UserContext for use throughout the request
                userContext.setUserId(userId);
                userContext.setEmail(email);
                userContext.setTenantId(tenantId);
                userContext.setUserType(userType);
                userContext.setRoles(roles);

                // Create authentication object
                JwtAuthentication authentication = new JwtAuthentication(userId, email, tenantId, userType, roles);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log the exception if needed
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}