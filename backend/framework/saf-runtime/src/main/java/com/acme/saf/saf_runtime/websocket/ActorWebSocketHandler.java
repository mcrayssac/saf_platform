package com.acme.saf.saf_runtime.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * WebSocket handler for actor communication.
 * Framework component - handles WebSocket connections for any actor.
 * 
 * Endpoint: ws://host:port/ws/actors/{actorId}
 */
@Component
public class ActorWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ActorWebSocketHandler.class);
    
    @Autowired
    private ActorWebSocketManager manager;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String actorId = extractActorId(session);
        if (actorId != null) {
            manager.registerSession(actorId, session);
            log.info("WebSocket connected for actor: {}", actorId);
        } else {
            log.warn("Could not extract actor ID from WebSocket session, closing connection");
            session.close(CloseStatus.BAD_DATA);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String actorId = extractActorId(session);
        if (actorId != null) {
            manager.unregisterSession(actorId);
            log.info("WebSocket disconnected for actor: {} (status: {})", actorId, status);
        }
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Optional: handle incoming messages from client
        // For now, actors push messages to clients (one-way communication)
        String actorId = extractActorId(session);
        log.debug("Received message from actor {}: {}", actorId, message.getPayload());
        
        // Applications can extend this to handle client->actor messages
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String actorId = extractActorId(session);
        log.error("WebSocket transport error for actor: {}", actorId, exception);
        manager.unregisterSession(actorId);
    }
    
    /**
     * Extract actor ID from WebSocket session URI.
     * Expected format: /ws/actors/{actorId}
     * 
     * @param session The WebSocket session
     * @return The actor ID, or null if not found
     */
    private String extractActorId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                return null;
            }
            
            String path = uri.getPath();
            // Path format: /ws/actors/{actorId}
            if (path != null && path.startsWith("/ws/actors/")) {
                return path.substring("/ws/actors/".length());
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting actor ID from WebSocket session", e);
            return null;
        }
    }
}
