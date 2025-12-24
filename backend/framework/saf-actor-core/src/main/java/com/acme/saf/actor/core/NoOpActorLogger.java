package com.acme.saf.actor.core;

import java.util.Map;

/**
 * NoOpActorLogger is a no-operation implementation of ActorLogger.
 * Used as the default when no logging is configured.
 * 
 * All methods are empty - no actual logging occurs.
 * This avoids null checks and provides a safe default.
 * 
 * Usage:
 * <pre>
 * // Default logger when none configured
 * ActorLogger logger = new NoOpActorLogger();
 * logger.logCreation("actor-1", "CLIENT", null); // No-op
 * </pre>
 */
public class NoOpActorLogger implements ActorLogger {
    
    private static final NoOpActorLogger INSTANCE = new NoOpActorLogger();
    
    /**
     * Gets the singleton instance of NoOpActorLogger.
     * 
     * @return the singleton instance
     */
    public static NoOpActorLogger getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void logCreation(String actorId, String actorType, String correlationId) {
        // No-op
    }
    
    @Override
    public void logCreation(String actorId, String actorType, String correlationId, Map<String, Object> metadata) {
        // No-op
    }
    
    @Override
    public void logDestruction(String actorId, String correlationId) {
        // No-op
    }
    
    @Override
    public void logMessage(String fromActorId, String toActorId, Message message, String correlationId) {
        // No-op
    }
    
    @Override
    public void logMessageFlow(String actorId, String direction, Message message, String correlationId) {
        // No-op
    }
    
    @Override
    public void logStateChange(String actorId, ActorLifecycleState oldState, ActorLifecycleState newState, String correlationId) {
        // No-op
    }
    
    @Override
    public void logError(String actorId, Throwable cause, String correlationId) {
        // No-op
    }
    
    @Override
    public void logError(String actorId, String message, Throwable cause, String correlationId) {
        // No-op
    }
    
    @Override
    public void debug(String actorId, String message, String correlationId) {
        // No-op
    }
    
    @Override
    public void info(String actorId, String message, String correlationId) {
        // No-op
    }
    
    @Override
    public void warn(String actorId, String message, String correlationId) {
        // No-op
    }
    
    @Override
    public void logSupervision(String supervisorId, String childId, String directive, Throwable cause, String correlationId) {
        // No-op
    }
    
    @Override
    public void logMetric(String actorId, String metricName, Number value, String correlationId) {
        // No-op
    }
    
    @Override
    public ActorLogger withCorrelationId(String correlationId) {
        return this; // Return self since all operations are no-op anyway
    }
    
    @Override
    public boolean isDebugEnabled() {
        return false; // Debug logging is never enabled for no-op logger
    }
}
