package com.acme.saf.saf_control.registry;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of microservices in the distributed actor system.
 * Maintains information about available services and their URLs.
 */
@Slf4j
@Component
public class ServiceRegistry {
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    
    /**
     * Register a service
     */
    public void registerService(String serviceId, String serviceUrl) {
        ServiceInfo info = new ServiceInfo(serviceId, serviceUrl);
        services.put(serviceId, info);
        log.info("Service registered: id={}, url={}", serviceId, serviceUrl);
    }
    
    /**
     * Get service information
     */
    public Optional<ServiceInfo> getService(String serviceId) {
        return Optional.ofNullable(services.get(serviceId));
    }
    
    /**
     * Get service URL
     */
    public Optional<String> getServiceUrl(String serviceId) {
        return getService(serviceId).map(ServiceInfo::getUrl);
    }
    
    /**
     * Unregister a service
     */
    public void unregisterService(String serviceId) {
        ServiceInfo removed = services.remove(serviceId);
        if (removed != null) {
            log.info("Service unregistered: id={}", serviceId);
        }
    }
    
    /**
     * Get all registered services
     */
    public Collection<ServiceInfo> getAllServices() {
        return services.values();
    }
    
    /**
     * Check if a service is registered
     */
    public boolean isRegistered(String serviceId) {
        return services.containsKey(serviceId);
    }
    
    /**
     * Update service heartbeat
     */
    public void updateHeartbeat(String serviceId) {
        ServiceInfo info = services.get(serviceId);
        if (info != null) {
            info.setLastHeartbeat(System.currentTimeMillis());
        }
    }
    
    /**
     * Information about a registered service
     */
    @Data
    public static class ServiceInfo {
        private final String serviceId;
        private final String url;
        private final long registeredAt;
        private long lastHeartbeat;
        private boolean active;
        
        public ServiceInfo(String serviceId, String url) {
            this.serviceId = serviceId;
            this.url = url;
            this.registeredAt = System.currentTimeMillis();
            this.lastHeartbeat = this.registeredAt;
            this.active = true;
        }
    }
}
