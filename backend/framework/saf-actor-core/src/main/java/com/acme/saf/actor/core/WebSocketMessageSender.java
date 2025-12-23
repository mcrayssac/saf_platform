package com.acme.saf.actor.core;

/**
 * Interface for sending messages to WebSocket clients.
 * This is a framework interface that allows decoupling between
 * saf-actor-core (which defines the interface) and saf-runtime
 * (which implements the WebSocket functionality).
 * 
 * This pattern allows ActorContext to support WebSocket without
 * depending on Spring WebSocket libraries.
 */
public interface WebSocketMessageSender {
    
    /**
     * Send a message to an actor's WebSocket client.
     * The message will be JSON serialized automatically.
     * 
     * @param actorId The actor ID
     * @param message The message to send (will be JSON serialized)
     */
    void sendToActor(String actorId, Object message);
    
    /**
     * Check if an actor has an active WebSocket connection.
     * 
     * @param actorId The actor ID
     * @return true if a WebSocket session exists and is open
     */
    boolean hasSession(String actorId);
}
