package com.acme.saf.saf_runtime.websocket;

import com.acme.saf.actor.core.WebSocketMessageSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic WebSocket manager for actor-to-client communication.
 * Framework component - application agnostic.
 * 
 * This manager maintains WebSocket sessions for actors and provides
 * methods to send messages to connected clients.
 * 
 * Implements WebSocketMessageSender to allow ActorContext to use WebSocket
 * without depending on Spring WebSocket libraries.
 */
@Component
public class ActorWebSocketManager implements WebSocketMessageSender {
    
    private static final Logger log = LoggerFactory.getLogger(ActorWebSocketManager.class);
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Register a WebSocket session for an actor.
     * 
     * @param actorId The actor ID
     * @param session The WebSocket session
     */
    public void registerSession(String actorId, WebSocketSession session) {
        sessions.put(actorId, session);
        log.info("WebSocket session registered for actor: {}", actorId);
    }
    
    /**
     * Unregister a WebSocket session.
     * 
     * @param actorId The actor ID
     */
    public void unregisterSession(String actorId) {
        WebSocketSession session = sessions.remove(actorId);
        if (session != null) {
            log.info("WebSocket session unregistered for actor: {}", actorId);
        }
    }
    
    /**
     * Check if an actor has an active WebSocket session.
     * 
     * @param actorId The actor ID
     * @return true if session exists and is open
     */
    public boolean hasSession(String actorId) {
        WebSocketSession session = sessions.get(actorId);
        return session != null && session.isOpen();
    }
    
    /**
     * Send a message to a specific actor's WebSocket client.
     * The message will be JSON serialized automatically.
     * 
     * @param actorId The actor ID
     * @param message The message to send (will be JSON serialized)
     */
    public void sendToActor(String actorId, Object message) {
        WebSocketSession session = sessions.get(actorId);
        
        if (session == null) {
            log.debug("No WebSocket session found for actor: {}", actorId);
            return;
        }
        
        if (!session.isOpen()) {
            log.warn("WebSocket session is closed for actor: {}", actorId);
            sessions.remove(actorId);
            return;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("Message sent to actor {} via WebSocket", actorId);
        } catch (IOException e) {
            log.error("Failed to send message to actor {} via WebSocket", actorId, e);
            // Remove session if sending fails
            sessions.remove(actorId);
        }
    }
    
    /**
     * Broadcast a message to multiple actors.
     * 
     * @param actorIds List of actor IDs
     * @param message The message to broadcast
     */
    public void broadcast(List<String> actorIds, Object message) {
        if (actorIds == null || actorIds.isEmpty()) {
            return;
        }
        
        for (String actorId : actorIds) {
            sendToActor(actorId, message);
        }
    }
    
    /**
     * Get the number of active WebSocket sessions.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Close all WebSocket sessions (for shutdown).
     */
    public void closeAllSessions() {
        log.info("Closing all WebSocket sessions...");
        sessions.forEach((actorId, session) -> {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                log.error("Error closing WebSocket session for actor: {}", actorId, e);
            }
        });
        sessions.clear();
        log.info("All WebSocket sessions closed");
    }
}
