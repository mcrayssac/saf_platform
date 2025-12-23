package com.acme.iot.capteur.config;

import com.acme.saf.saf_runtime.config.BaseServiceRegistrationInitializer;
import org.springframework.stereotype.Component;

/**
 * Initializes service registration for capteur-service with saf-control.
 * Extends BaseServiceRegistrationInitializer to handle the registration automatically.
 */
@Component
public class ServiceRegistrationInitializer extends BaseServiceRegistrationInitializer {
    // No constructor needed - base class uses @Value annotations
}
