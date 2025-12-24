package com.acme.saf.actor.core;

/**
 * ActorContext provides contextual information and utilities to actors.
 * Available to actors during message processing via the receive() method.
 * 
 * Features:
 * - Actor identification (self, sender)
 * - Simple logging methods (backward compatible)
 * - Structured logging with ActorLogger
 * - Event publishing for lifecycle events
 * - Correlation ID tracking for distributed tracing
 * 
 * Example:
 * <pre>
 * public void receive(Message message, ActorContext context) {
 *     context.logInfo("Processing message");
 *     
 *     // Use structured logger
 *     ActorLogger logger = context.getLogger();
 *     logger.logMessage(context.sender().getActorId(), 
 *                      context.self().getActorId(), 
 *                      message, 
 *                      context.getCorrelationId());
 * }
 * </pre>
 */
public interface ActorContext {
    // Identification
    
    /**
     * Gets a reference to the current actor (self).
     * 
     * @return reference to this actor
     */
    ActorRef self();
    
    /**
     * Gets a reference to the sender of the current message.
     * May be null if message was sent without a sender reference.
     * 
     * @return reference to the sender, or null
     */
    ActorRef sender();

    // Simple Logging (Backward Compatible)
    
    /**
     * Logs an informational message.
     * 
     * @param message the message to log
     */
    void logInfo(String message);
    
    /**
     * Logs a warning message.
     * 
     * @param message the warning message
     */
    void logWarning(String message);
    
    /**
     * Logs an error message with exception.
     * 
     * @param message the error message
     * @param t the throwable/exception
     */
    void logError(String message, Throwable t);
    
    // Structured Logging
    
    /**
     * Gets the structured ActorLogger for this context.
     * Provides comprehensive logging capabilities with correlation IDs.
     * 
     * @return the actor logger
     */
    ActorLogger getLogger();
    
    /**
     * Gets the correlation ID for the current message processing.
     * Used for distributed tracing across actors and services.
     * May be null if no correlation ID is set.
     * 
     * @return the correlation ID, or null
     */
    String getCorrelationId();
    
    /**
     * Sets the correlation ID for the current message processing.
     * Should be called early in message handling to enable tracing.
     * 
     * @param correlationId the correlation ID to set
     */
    void setCorrelationId(String correlationId);

    // Event Publishing
    
    /**
     * Publishes an actor lifecycle event.
     * Events are broadcast to registered listeners/observers.
     * 
     * @param event the lifecycle event to publish
     */
    void publishEvent(ActorLifecycleEvent event);
    
    // WebSocket Communication
    
    /**
     * Send a message to this actor's WebSocket client (if connected).
     * The message will be automatically JSON serialized and sent to the client.
     * This is a generic framework method - applications decide what message to send.
     * 
     * If no WebSocket connection exists for this actor, the message is silently ignored.
     * 
     * @param message The message object (will be JSON serialized)
     */
    void sendToWebSocket(Object message);
    
    /**
     * Check if this actor has an active WebSocket connection.
     * 
     * @return true if a WebSocket connection exists and is open
     */
    boolean hasWebSocketConnection();
    
    // Actor Lookup (Generic Framework Feature)
    
    /**
     * Look up an actor by its ID.
     * Generic framework method - allows actors to find and communicate with each other.
     * 
     * @param actorId The ID of the actor to find
     * @return ActorRef if found, null otherwise
     */
    ActorRef actorFor(String actorId);
}
