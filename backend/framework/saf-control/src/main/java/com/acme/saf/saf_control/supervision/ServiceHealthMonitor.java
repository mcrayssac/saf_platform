package com.acme.saf.saf_control.supervision;

import com.acme.saf.saf_control.events.ServiceDownEvent;
import com.acme.saf.saf_control.events.ServiceRecoveredEvent;
import com.acme.saf.saf_control.registry.DistributedActorRegistry;
import com.acme.saf.saf_control.registry.ServiceRegistry;
import com.acme.saf.saf_control.registry.ServiceRegistry.ServiceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors the health of registered microservices.
 * Performs periodic health checks and marks services as down/up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceHealthMonitor {
    
    private final ServiceRegistry serviceRegistry;
    private final DistributedActorRegistry actorRegistry;
    private final WebClient webClient;
    private final ApplicationEventPublisher eventPublisher;
    
    // Track which services were previously down to detect recovery
    private final Map<String, Boolean> serviceHealthStatus = new ConcurrentHashMap<>();
    
    // Configuration
    private static final long HEARTBEAT_TIMEOUT_MS = 60_000; // 60 seconds
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    
    /**
     * Periodic health check for all registered services.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedRate = 10_000, initialDelay = 15_000)
    public void checkServicesHealth() {
        log.debug("Starting health check for {} registered services", 
                serviceRegistry.getAllServices().size());
        
        for (ServiceInfo service : serviceRegistry.getAllServices()) {
            checkServiceHealth(service);
        }
    }
    
    /**
     * Check the health of a single service
     */
    private void checkServiceHealth(ServiceInfo service) {
        String serviceId = service.getServiceId();
        
        // 1. Check heartbeat age
        if (isHeartbeatStale(service)) {
            log.warn("Service {} has stale heartbeat (age: {}ms)", 
                    serviceId, System.currentTimeMillis() - service.getLastHeartbeat());
        }
        
        // 2. Active health check via HTTP
        performActiveHealthCheck(service)
                .subscribe(
                        healthy -> handleServiceHealthy(service),
                        error -> handleServiceDown(service, error)
                );
    }
    
    /**
     * Check if the service's heartbeat is stale
     */
    private boolean isHeartbeatStale(ServiceInfo service) {
        long age = System.currentTimeMillis() - service.getLastHeartbeat();
        return age > HEARTBEAT_TIMEOUT_MS;
    }
    
    /**
     * Perform an active health check by calling the service's /actuator/health endpoint
     */
    private Mono<Boolean> performActiveHealthCheck(ServiceInfo service) {
        String healthUrl = service.getUrl() + "/actuator/health";
        
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(HEALTH_CHECK_TIMEOUT)
                .onErrorResume(error -> {
                    log.debug("Health check failed for {}: {}", 
                            service.getServiceId(), error.getMessage());
                    return Mono.just(false);
                })
                .flatMap(healthy -> {
                    if (healthy) {
                        return Mono.just(true);
                    } else {
                        return Mono.error(new RuntimeException("Health check returned unhealthy"));
                    }
                });
    }
    
    /**
     * Handle a service that is healthy
     */
    private void handleServiceHealthy(ServiceInfo service) {
        String serviceId = service.getServiceId();
        Boolean previouslyDown = serviceHealthStatus.get(serviceId);
        
        // Check if service is recovering from a down state
        if (Boolean.FALSE.equals(previouslyDown)) {
            handleServiceRecovery(service);
        }
        
        // Mark service as active
        service.setActive(true);
        serviceHealthStatus.put(serviceId, true);
        
        log.trace("Service {} is healthy", serviceId);
    }
    
    /**
     * Handle a service that is down
     */
    private void handleServiceDown(ServiceInfo service, Throwable error) {
        String serviceId = service.getServiceId();
        Boolean previouslyUp = serviceHealthStatus.get(serviceId);
        
        // Only log and act if this is a state transition (UP -> DOWN)
        if (!Boolean.FALSE.equals(previouslyUp)) {
            log.error("Service {} is DOWN: {}", serviceId, error.getMessage());
            
            // 1. Mark service as inactive
            service.setActive(false);
            
            // 2. Mark all actors from this service as unavailable
            int actorsAffected = actorRegistry.markActorsUnavailable(serviceId);
            log.warn("Marked {} actors as unavailable due to service {} failure", 
                    actorsAffected, serviceId);
            
            // 3. Emit event for monitoring and alerting
            ServiceDownEvent event = new ServiceDownEvent(this, service, error.getMessage());
            eventPublisher.publishEvent(event);
            
            // 4. Track the down state
            serviceHealthStatus.put(serviceId, false);
        } else {
            log.debug("Service {} remains down", serviceId);
        }
    }
    
    /**
     * Handle a service recovering from a down state
     */
    private void handleServiceRecovery(ServiceInfo service) {
        String serviceId = service.getServiceId();
        log.info("Service {} has RECOVERED - performing state reconciliation", serviceId);
        
        // 1. Mark service as active
        service.setActive(true);
        
        // 2. Query the service for its actual running actors
        String actorsListUrl = service.getUrl() + "/runtime/actors";
        
        webClient.get()
                .uri(actorsListUrl)
                .retrieve()
                .bodyToMono(java.util.List.class)
                .timeout(HEALTH_CHECK_TIMEOUT)
                .subscribe(
                        actualActorIds -> {
                            // 3. Reconcile registry with actual running actors
                            DistributedActorRegistry.ReconciliationResult result = 
                                    actorRegistry.reconcileActors(serviceId, (java.util.List<String>) actualActorIds);
                            
                            log.info("State reconciliation for service {}: {} orphans removed, {} actors restored, {} kept running",
                                    serviceId, result.getRemoved(), result.getRestored(), result.getKept());
                            
                            // 4. Emit recovery event
                            ServiceRecoveredEvent event = new ServiceRecoveredEvent(this, service);
                            eventPublisher.publishEvent(event);
                        },
                        error -> {
                            log.error("Failed to fetch actors list from {} during recovery: {}", 
                                    serviceId, error.getMessage());
                            // Fallback to old behavior if actor list fetch fails
                            int actorsRecovered = actorRegistry.markActorsAvailable(serviceId);
                            log.warn("Using fallback recovery (no reconciliation) for service {}: {} actors marked available", 
                                    serviceId, actorsRecovered);
                            
                            ServiceRecoveredEvent event = new ServiceRecoveredEvent(this, service);
                            eventPublisher.publishEvent(event);
                        }
                );
        
        // 5. Update health status
        serviceHealthStatus.put(serviceId, true);
    }
    
    /**
     * Get the current health status of a service
     */
    public boolean isServiceHealthy(String serviceId) {
        return Boolean.TRUE.equals(serviceHealthStatus.get(serviceId));
    }
}
