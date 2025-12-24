package com.acme.saf.saf_control.events;

import com.acme.saf.saf_control.registry.ServiceRegistry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a service recovers after being down
 */
@Getter
public class ServiceRecoveredEvent extends ApplicationEvent {
    
    private final String serviceId;
    private final String serviceUrl;
    
    public ServiceRecoveredEvent(Object source, ServiceRegistry.ServiceInfo service) {
        super(source);
        this.serviceId = service.getServiceId();
        this.serviceUrl = service.getUrl();
    }
}
