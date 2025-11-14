package com.acme.saf.saf_control.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    private static final String API_KEY_NAME = "X-API-KEY";
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAF Control API")
                        .version("1.0")
                        .description("SAF Control Service API with API Key authentication"))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_NAME))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_NAME, new SecurityScheme()
                                .name(API_KEY_NAME)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
