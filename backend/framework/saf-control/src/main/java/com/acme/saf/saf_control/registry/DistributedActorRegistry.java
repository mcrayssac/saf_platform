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
     * Mark all actors from a specific service as unavailable (when service goes down)
     */
    public int markActorsUnavailable(String serviceId) {
        Collection<ActorRegistryEntry> serviceActors = getActorsByService(serviceId);
        int count = 0;
        
        for (ActorRegistryEntry actor : serviceActors) {
            actor.setState(ActorLifecycleState.STOPPED);
            count++;
        }
        
        if (count > 0) {
            log.warn("Marked {} actors as unavailable for service: {}", count, serviceId);
        }
        
        return count;
    }
    
    /**
     * Mark all actors from a specific service as available (when service recovers)
     * @deprecated Use reconcileActors() instead for proper state reconciliation
     */
    @Deprecated
    public int markActorsAvailable(String serviceId) {
        Collection<ActorRegistryEntry> serviceActors = getActorsByService(serviceId);
        int count = 0;
        
        for (ActorRegistryEntry actor : serviceActors) {
            if (actor.getState() == ActorLifecycleState.STOPPED) {
                actor.setState(ActorLifecycleState.RUNNING);
                count++;
            }
        }
        
        if (count > 0) {
            log.info("Marked {} actors as available for service: {}", count, serviceId);
        }
        
        return count;
    }
    
    /**
     * Reconcile actors for a service with the actual actors running on that service.
     * This removes orphaned actors that don't exist anymore and keeps only real ones.
     * 
     * @param serviceId The service ID
     * @param actualActorIds The list of actor IDs actually running on the service
     * @return Number of actors removed (orphans) and number kept
     */
    public ReconciliationResult reconcileActors(String serviceId, Collection<String> actualActorIds) {
        Collection<ActorRegistryEntry> registeredActors = getActorsByService(serviceId);
        
        int removed = 0;
        int kept = 0;
        int restored = 0;
        
        // Remove actors that no longer exist on the service
        for (ActorRegistryEntry actor : registeredActors) {
            if (!actualActorIds.contains(actor.getActorId())) {
                // Actor doesn't exist anymore - remove it
                registry.remove(actor.getActorId());
                removed++;
                log.warn("Removed orphaned actor {} from registry (no longer exists on service {})", 
                        actor.getActorId(), serviceId);
            } else {
                // Actor exists - mark it as RUNNING if it was STOPPED
                if (actor.getState() == ActorLifecycleState.STOPPED) {
                    actor.setState(ActorLifecycleState.RUNNING);
                    restored++;
                    log.info("Restored actor {} to RUNNING state", actor.getActorId());
                } else {
                    kept++;
                }
            }
        }
        
        log.info("Reconciliation for service {}: {} removed (orphans), {} restored, {} kept", 
                serviceId, removed, restored, kept);
        
        return new ReconciliationResult(removed, restored, kept);
    }
    
    /**
     * Result of a reconciliation operation
     */
    public static class ReconciliationResult {
        private final int removed;
        private final int restored;
        private final int kept;
        
        public ReconciliationResult(int removed, int restored, int kept) {
            this.removed = removed;
            this.restored = restored;
            this.kept = kept;
        }
        
        public int getRemoved() {
            return removed;
        }
        
        public int getRestored() {
            return restored;
        }
        
        public int getKept() {
            return kept;
        }
        
        public int getTotal() {
            return restored + kept;
        }
    }
    
    /**
     * Clear the entire registry (use with caution!)
     */
    public void clear() {
        registry.clear();
        log.warn("Actor registry cleared");
    }
}
