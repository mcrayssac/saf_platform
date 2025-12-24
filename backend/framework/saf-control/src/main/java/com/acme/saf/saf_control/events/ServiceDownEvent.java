package com.acme.saf.saf_control.events;

import com.acme.saf.saf_control.registry.ServiceRegistry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a service is detected as down
 */
@Getter
public class ServiceDownEvent extends ApplicationEvent {
    
    private final String serviceId;
    private final String serviceUrl;
    private final String reason;
    
    public ServiceDownEvent(Object source, ServiceRegistry.ServiceInfo service, String reason) {
        super(source);
        this.serviceId = service.getServiceId();
        this.serviceUrl = service.getUrl();
        this.reason = reason;
    }
}
