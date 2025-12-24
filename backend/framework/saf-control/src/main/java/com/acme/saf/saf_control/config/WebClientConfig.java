package com.acme.saf.saf_control.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used for inter-service communication.
 */
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
