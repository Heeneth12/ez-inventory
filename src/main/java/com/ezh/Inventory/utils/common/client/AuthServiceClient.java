package com.ezh.Inventory.utils.common.client;

import com.ezh.Inventory.utils.common.ExternalApiResponse;
import com.ezh.Inventory.utils.common.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${service.internal.secret:}")
    private String internalSecret;

    public UserDto getUserDetailsById(Long userId) {
        URI uri = UriComponentsBuilder.fromUriString(authServiceUrl)
                .path("/api/v1/user/{id}")
                .buildAndExpand(userId)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        headers.setBearerAuth(token.replace("Bearer ", ""));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

        // 2. Wrap into HttpEntity (For GET, the body is null)
        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.info("Calling Auth Service URL: {}", uri);

        ResponseEntity<ExternalApiResponse<UserDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ExternalApiResponse<UserDto>>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Received successful response from Auth Service for userId: {}", userId);
            return response.getBody().getData();
        } else {
            log.error("Failed to fetch user details from Auth Service for userId: {}. Status Code: {}, Response Body: {}",
                    userId, response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to fetch user details from Auth Service");
        }
    }


    public Map<Long, UserMiniDto> getBulkUserDetails(List<Long> ids) {
        return getBulkUserDetails(ids, false);
    }

    public Map<Long, UserMiniDto> getBulkUserDetails(List<Long> ids, Boolean includeAddress) {
        URI uri = UriComponentsBuilder.fromUriString(authServiceUrl)
                .path("/api/v1/user/bulk")
                .queryParam("ids", ids)
                .queryParam("address", includeAddress)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        headers.setBearerAuth(token.replace("Bearer ", ""));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. Use ParameterizedTypeReference to capture the Map inside the ResponseResource
        ResponseEntity<ExternalApiResponse<Map<Long, UserMiniDto>>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ExternalApiResponse<Map<Long, UserMiniDto>>>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Received successful response from Auth Service for userId: {}", response.getBody().getData());
            return response.getBody().getData();
        } else {
            log.error("Failed bulk fetch. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to fetch bulk user details");
        }
    }


    public TenantDto getTenantById(Long tenantId) {
        URI uri = UriComponentsBuilder.fromUriString(authServiceUrl)
                .path("/api/v1/tenant/{id}")
                .buildAndExpand(tenantId)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        headers.setBearerAuth(token.replace("Bearer ", ""));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.info("Calling Auth Service URL to fetch tenant: {}", uri);

        ResponseEntity<ExternalApiResponse<TenantDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ExternalApiResponse<TenantDto>>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Received successful response from Auth Service for tenantId: {}", tenantId);
            return response.getBody().getData();
        } else {
            log.error("Failed to fetch tenant details from Auth Service for tenantId: {}. Status Code: {}, Response Body: {}",
                    tenantId, response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to fetch tenant details from Auth Service");
        }
    }


    /**
     * Fetches a specific integration (like RAZORPAY) from the Auth Service
     * for the current tenant.
     */
    public IntegrationDto getIntegrationByType(IntegrationType type) {
        URI uri = UriComponentsBuilder.fromUriString(authServiceUrl)
                .path("/api/v1/integrations/by-type") // Adjust path to match your Auth Controller
                .queryParam("type", type)
                .build()
                .toUri();

        HttpHeaders headers = createAuthHeaders();

        log.info("Calling Auth Service to fetch integration: {} for current tenant", type);

        ResponseEntity<ExternalApiResponse<IntegrationDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ExternalApiResponse<IntegrationDto>>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            log.error("Failed to fetch integration {}. Status: {}", type, response.getStatusCode());
            throw new RuntimeException("Integration configuration not found for: " + type);
        }
    }


    /**
     * Fetches a tenant's Razorpay integration without a user JWT.
     * Used in webhook flows where no HTTP request context is available.
     * Sends {@code X-Internal-Secret} and {@code X-Tenant-Id} headers so the
     * auth service can authenticate the internal call and scope the response.
     */
    public IntegrationDto getIntegrationByTypeForTenant(IntegrationType type, Long tenantId) {
        URI uri = UriComponentsBuilder.fromUriString(authServiceUrl)
                .path("/api/v1/integrations/by-type")
                .queryParam("type", type)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);
        headers.set("X-Tenant-Id", String.valueOf(tenantId));

        log.info("Calling Auth Service (internal) to fetch integration: {} for tenantId: {}", type, tenantId);

        ResponseEntity<ExternalApiResponse<IntegrationDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ExternalApiResponse<IntegrationDto>>() {
                }
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getData();
        } else {
            log.error("Failed to fetch integration {} for tenantId {}. Status: {}", type, tenantId, response.getStatusCode());
            throw new RuntimeException("Integration configuration not found for tenantId=" + tenantId + ", type=" + type);
        }
    }

    /**
     * Helper to avoid repeating header logic
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        if (token != null) {
            headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
