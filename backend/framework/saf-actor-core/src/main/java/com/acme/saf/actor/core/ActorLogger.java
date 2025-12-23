package com.acme.saf.actor.core;

import java.util.Map;

/**
 * ActorLogger provides structured logging for actor system events.
 * Enables comprehensive tracing of actor lifecycle, messages, state transitions, and errors.
 * 
 * This interface abstracts the logging implementation, allowing different backends
 * (SLF4J, Log4j, custom loggers, centralized logging services, etc.).
 * 
 * Features:
 * - Structured logging with context (actorId, type, metadata)
 * - Correlation IDs for distributed tracing
 * - Log levels (DEBUG, INFO, WARN, ERROR)
 * - Activity tracing (lifecycle, messages, state changes)
 * - Error tracking with stack traces
 * 
 * Example:
 * <pre>
 * ActorLogger logger = new Slf4jActorLogger();
 * 
 * // Log actor creation
 * logger.logCreation("client-123", "CLIENT", correlationId);
 * 
 * // Log message flow
 * logger.logMessage("sender-1", "receiver-2", message, correlationId);
 * 
 * // Log state changes
 * logger.logStateChange("client-123", RUNNING, STOPPED, correlationId);
 * </pre>
 */
public interface ActorLogger {
    
    /**
     * Logs actor creation event.
     * 
     * @param actorId the unique actor identifier
     * @param actorType the actor type (e.g., "CLIENT", "VILLE")
     * @param correlationId optional correlation ID for request tracing
     */
    void logCreation(String actorId, String actorType, String correlationId);
    
    /**
     * Logs actor creation with additional metadata.
     * 
     * @param actorId the unique actor identifier
     * @param actorType the actor type
     * @param correlationId optional correlation ID
     * @param metadata additional context (e.g., params, configuration)
     */
    void logCreation(String actorId, String actorType, String correlationId, Map<String, Object> metadata);
    
    /**
     * Logs actor destruction/stop event.
     * 
     * @param actorId the actor identifier
     * @param correlationId optional correlation ID
     */
    void logDestruction(String actorId, String correlationId);
    
    /**
     * Logs a message being sent between actors.
     * 
     * @param fromActorId the sender actor ID (null if external)
     * @param toActorId the receiver actor ID
     * @param message the message being sent
     * @param correlationId optional correlation ID for tracking message chains
     */
    void logMessage(String fromActorId, String toActorId, Message message, String correlationId);
    
    /**
     * Logs a message with direction indicator.
     * 
     * @param actorId the actor ID
     * @param direction "SEND" or "RECEIVE"
     * @param message the message
     * @param correlationId optional correlation ID
     */
    void logMessageFlow(String actorId, String direction, Message message, String correlationId);
    
    /**
     * Logs an actor state transition.
     * 
     * @param actorId the actor identifier
     * @param oldState the previous state
     * @param newState the new state
     * @param correlationId optional correlation ID
     */
    void logStateChange(String actorId, ActorLifecycleState oldState, ActorLifecycleState newState, String correlationId);
    
    /**
     * Logs an error or exception in an actor.
     * 
     * @param actorId the actor identifier
     * @param cause the exception/error
     * @param correlationId optional correlation ID
     */
    void logError(String actorId, Throwable cause, String correlationId);
    
    /**
     * Logs an error with additional context.
     * 
     * @param actorId the actor identifier
     * @param message descriptive error message
     * @param cause the exception/error
     * @param correlationId optional correlation ID
     */
    void logError(String actorId, String message, Throwable cause, String correlationId);
    
    /**
     * Logs a debug-level event for detailed troubleshooting.
     * 
     * @param actorId the actor identifier
     * @param message the debug message
     * @param correlationId optional correlation ID
     */
    void debug(String actorId, String message, String correlationId);
    
    /**
     * Logs an info-level event for general information.
     * 
     * @param actorId the actor identifier
     * @param message the info message
     * @param correlationId optional correlation ID
     */
    void info(String actorId, String message, String correlationId);
    
    /**
     * Logs a warning-level event.
     * 
     * @param actorId the actor identifier
     * @param message the warning message
     * @param correlationId optional correlation ID
     */
    void warn(String actorId, String message, String correlationId);
    
    /**
     * Logs actor supervision decision.
     * 
     * @param supervisorId the supervisor actor ID
     * @param childId the child actor ID
     * @param directive the supervision directive (RESTART, RESUME, STOP, ESCALATE)
     * @param cause the exception that triggered supervision
     * @param correlationId optional correlation ID
     */
    void logSupervision(String supervisorId, String childId, String directive, Throwable cause, String correlationId);
    
    /**
     * Logs a metric/measurement for monitoring.
     * 
     * @param actorId the actor identifier
     * @param metricName the metric name (e.g., "processing_time", "queue_depth")
     * @param value the metric value
     * @param correlationId optional correlation ID
     */
    void logMetric(String actorId, String metricName, Number value, String correlationId);
    
    /**
     * Creates a child logger with a specific correlation ID.
     * Useful for maintaining correlation context across multiple log calls.
     * 
     * @param correlationId the correlation ID to use
     * @return a new logger instance with the correlation ID set
     */
    ActorLogger withCorrelationId(String correlationId);
    
    /**
     * Checks if debug logging is enabled.
     * Can be used to avoid expensive log message construction.
     * 
     * @return true if debug logging is enabled
     */
    boolean isDebugEnabled();
}
