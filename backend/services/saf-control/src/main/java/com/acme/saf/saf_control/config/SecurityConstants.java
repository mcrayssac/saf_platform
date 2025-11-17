package com.acme.saf.saf_control.config;

public class SecurityConstants {
    
    public static final String[] PUBLIC_PATHS = {
        "/actuator/**",
        "/swagger/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/agents/**"
    };
    
    private SecurityConstants() {
        // Utility class
    }
}
