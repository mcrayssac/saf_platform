package com.acme.saf.saf_runtime.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for service registration with saf-control.
 * All microservices should extend this class to automatically
 * register themselves with the central control service on startup.
 */
public abstract class BaseServiceRegistrationInitializer implements ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(BaseServiceRegistrationInitializer.class);
    
    @Value("${saf-control.url}")
    private String safControlUrl;
    
    @Value("${saf-control.service-id}")
    private String serviceId;
    
    @Value("${saf-control.service-url}")
    private String serviceUrl;
    
    @Value("${saf-control.api-key:mock-secret}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private boolean registered = false;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Ensure we only register once (ContextRefreshedEvent can fire multiple times)
        if (!registered) {
            registerWithSafControl();
            registered = true;
        }
    }
    
    protected void registerWithSafControl() {
        try {
            String registerUrl = safControlUrl + "/api/v1/services/register?serviceId=" + serviceId + "&serviceUrl=" + serviceUrl;
            
            // Add API key to headers (must match ApiKeyFilter.HEADER_NAME exactly: X-API-KEY)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            restTemplate.postForEntity(registerUrl, request, Void.class);
            log.info("[{}] Successfully registered with saf-control: serviceId={}, serviceUrl={}", serviceId, serviceId, serviceUrl);
            onRegistrationSuccess();
        } catch (Exception e) {
            log.error("[{}] Failed to register with saf-control", serviceId, e);
            onRegistrationFailure(e);
        }
    }
    
    /**
     * Hook method called after successful registration.
     * Override to perform additional initialization.
     */
    protected void onRegistrationSuccess() {
        // Override if needed
    }
    
    /**
     * Hook method called after registration failure.
     * Override to handle the error appropriately.
     */
    protected void onRegistrationFailure(Exception e) {
        // Override if needed
    }
    
    protected String getSafControlUrl() {
        return safControlUrl;
    }
    
    protected String getServiceId() {
        return serviceId;
    }
    
    protected String getServiceUrl() {
        return serviceUrl;
    }
    
    protected RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
