package com.acme.saf.saf_control.controller;

import com.acme.saf.saf_control.registry.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceRegistrationController {
    
    private final ServiceRegistry serviceRegistry;
    
    @PostMapping("/register")
    public ResponseEntity<Void> registerService(@RequestParam String serviceId, @RequestParam String serviceUrl) {
        log.info("Registering service: {} at {}", serviceId, serviceUrl);
        serviceRegistry.registerService(serviceId, serviceUrl);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/unregister")
    public ResponseEntity<Void> unregisterService(@RequestParam String serviceId) {
        log.info("Unregistering service: {}", serviceId);
        serviceRegistry.unregisterService(serviceId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{serviceId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String serviceId) {
        serviceRegistry.updateHeartbeat(serviceId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    public ResponseEntity<Collection<ServiceRegistry.ServiceInfo>> getAllServices() {
        return ResponseEntity.ok(serviceRegistry.getAllServices());
    }
    
    @GetMapping("/{serviceId}/registered")
    public ResponseEntity<Boolean> isServiceRegistered(@PathVariable String serviceId) {
        boolean registered = serviceRegistry.isRegistered(serviceId);
        log.debug("Service registration check: {} -> {}", serviceId, registered);
        return ResponseEntity.ok(registered);
    }
}
