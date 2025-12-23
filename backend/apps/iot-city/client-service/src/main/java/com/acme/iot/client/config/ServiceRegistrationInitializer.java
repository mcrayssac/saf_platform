package com.acme.iot.client.config;

import com.acme.saf.saf_runtime.config.BaseServiceRegistrationInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service registration initializer for client-service.
 * Extends BaseServiceRegistrationInitializer to automatically register
 * with saf-control on application startup.
 */
@Component
public class ServiceRegistrationInitializer extends BaseServiceRegistrationInitializer {
    // No additional implementation needed; uses base class functionality
}
