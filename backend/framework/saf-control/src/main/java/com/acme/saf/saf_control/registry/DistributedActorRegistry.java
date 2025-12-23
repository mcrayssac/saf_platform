package com.acme.saf.saf_control.registry;

import com.acme.saf.actor.core.ActorLifecycleState;
import com.acme.saf.actor.core.protocol.ActorRegistryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed actor registry maintained by saf-control.
 * Tracks all actors across all microservices in the system.
 */
@Slf4j
@Component
public class DistributedActorRegistry {
    
    private final Map<String, ActorRegistryEntry> registry = new ConcurrentHashMap<>();
    
    /**
     * Register a new actor in the distributed system
     */
    public void registerActor(String actorId, String actorType, String serviceId, String serviceUrl) {
        ActorRegistryEntry entry = new ActorRegistryEntry(actorId, actorType, serviceId, serviceUrl);
        entry.setState(ActorLifecycleState.RUNNING);
        
        registry.put(actorId, entry);
        
        log.info("Actor registered: id={}, type={}, service={}", actorId, actorType, serviceId);
    }
    
    /**
     * Register an actor using an entry object
     */
    public void registerActor(ActorRegistryEntry entry) {
        registry.put(entry.getActorId(), entry);
        log.info("Actor registered: id={}, type={}, service={}", 
                entry.getActorId(), entry.getActorType(), entry.getServiceId());
    }
    
    /**
     * Lookup an actor by ID
     */
    public Optional<ActorRegistryEntry> lookupActor(String actorId) {
        return Optional.ofNullable(registry.get(actorId));
    }
    
    /**
     * Update actor state
     */
    public void updateActorState(String actorId, ActorLifecycleState newState) {
        ActorRegistryEntry entry = registry.get(actorId);
        if (entry != null) {
            entry.setState(newState);
            log.info("Actor state updated: id={}, newState={}", actorId, newState);
        } else {
            log.warn("Cannot update state for unknown actor: {}", actorId);
        }
    }
    
    /**
     * Remove an actor from the registry
     */
    public void removeActor(String actorId) {
        ActorRegistryEntry removed = registry.remove(actorId);
        if (removed != null) {
            log.info("Actor removed: id={}, type={}, service={}", 
                    actorId, removed.getActorType(), removed.getServiceId());
        } else {
            log.warn("Cannot remove unknown actor: {}", actorId);
        }
    }
    
    /**
     * Get all actors for a specific service
     */
    public Collection<ActorRegistryEntry> getActorsByService(String serviceId) {
        return registry.values().stream()
                .filter(entry -> serviceId.equals(entry.getServiceId()))
                .toList();
    }
    
    /**
     * Get all active actors
     */
    public Collection<ActorRegistryEntry> getActiveActors() {
        return registry.values().stream()
                .filter(ActorRegistryEntry::isActive)
                .toList();
    }
    
    /**
     * Get all registered actors
     */
    public Collection<ActorRegistryEntry> getAllActors() {
        return registry.values();
    }
    
    /**
     * Check if an actor exists
     */
    public boolean exists(String actorId) {
        return registry.containsKey(actorId);
    }
    
    /**
     * Get the total number of registered actors
     */
    public int size() {
        return registry.size();
    }
    
    /**
     * Clear the entire registry (use with caution!)
     */
    public void clear() {
        registry.clear();
        log.warn("Actor registry cleared");
    }
}
