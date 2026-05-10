package com.ezh.Inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class RazorpayConfig {
    // Keep this for JSON operations
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}