package com.ezh.Inventory.utils.common.client;

import com.ezh.Inventory.utils.common.ExternalApiResponse;
import com.ezh.Inventory.utils.common.dto.UserDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${auth.service.url}")
    private String authServiceUrl;

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
                new ParameterizedTypeReference<ExternalApiResponse<UserDto>>() {}
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
}
