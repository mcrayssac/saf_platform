package com.acme.saf.saf_runtime.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Base class for service registration with saf-control.
 * All microservices should extend this class to automatically
 * register themselves with the central control service on startup.
 * 
 * Features:
 * - Initial registration on startup
 * - Periodic heartbeat to maintain registration
 * - Automatic re-registration if control restarts
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
    
    @Value("${saf-control.heartbeat.interval-seconds:30}")
    private int heartbeatIntervalSeconds;
    
    @Value("${saf-control.heartbeat.enabled:true}")
    private boolean heartbeatEnabled;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private boolean registered = false;
    private boolean initialRegistrationAttempted = false;
    
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
    
    /**
     * Periodic heartbeat task that:
     * 1. Checks if the service is still registered with saf-control
     * 2. If not registered or control is unavailable, attempts to re-register
     * 
     * Runs every X seconds (configurable via saf-control.heartbeat.interval-seconds)
     */
    @Scheduled(fixedDelayString = "${saf-control.heartbeat.interval-seconds:30}000", initialDelayString = "${saf-control.heartbeat.initial-delay-seconds:30}000")
    public void performHeartbeat() {
        if (!heartbeatEnabled) {
            return;
        }
        
        if (!initialRegistrationAttempted) {
            initialRegistrationAttempted = true;
            return; // Skip first heartbeat to allow initial registration to complete
        }
        
        try {
            // Check if we're still registered
            String checkUrl = safControlUrl + "/api/v1/services/" + serviceId + "/registered";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            Boolean isRegistered = restTemplate.exchange(checkUrl, org.springframework.http.HttpMethod.GET, request, Boolean.class).getBody();
            
            if (Boolean.TRUE.equals(isRegistered)) {
                // Still registered, send heartbeat
                sendHeartbeat();
                log.debug("[{}] Heartbeat sent successfully", serviceId);
            } else {
                // Not registered, attempt re-registration
                log.warn("[{}] Service is not registered with saf-control, attempting re-registration", serviceId);
                registerWithSafControl();
            }
        } catch (Exception e) {
            // Control might be down or unreachable, attempt to re-register
            log.warn("[{}] Failed to check registration status (control might be down), attempting re-registration: {}", 
                serviceId, e.getMessage());
            try {
                registerWithSafControl();
            } catch (Exception re) {
                log.error("[{}] Re-registration attempt failed: {}", serviceId, re.getMessage());
            }
        }
    }
    
    /**
     * Send heartbeat to saf-control to update last seen timestamp
     */
    private void sendHeartbeat() {
        try {
            String heartbeatUrl = safControlUrl + "/api/v1/services/" + serviceId + "/heartbeat";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            restTemplate.postForEntity(heartbeatUrl, request, Void.class);
        } catch (Exception e) {
            log.warn("[{}] Failed to send heartbeat: {}", serviceId, e.getMessage());
            throw e; // Re-throw to trigger re-registration logic
        }
    }
}
