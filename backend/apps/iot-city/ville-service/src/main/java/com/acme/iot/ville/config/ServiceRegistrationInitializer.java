package com.acme.iot.ville.config;

import com.acme.saf.saf_runtime.config.BaseServiceRegistrationInitializer;
import org.springframework.stereotype.Component;

/**
 * Service Registration Initializer for ville-service.
 * 
 * Automatically registers this service with saf-control on startup.
 * The service will be registered as "ville-service" and provide
 * actor type "VilleActor".
 */
@Component
public class ServiceRegistrationInitializer extends BaseServiceRegistrationInitializer {
    // All functionality inherited from BaseServiceRegistrationInitializer
    // Registration happens automatically using application.yml configuration
}
