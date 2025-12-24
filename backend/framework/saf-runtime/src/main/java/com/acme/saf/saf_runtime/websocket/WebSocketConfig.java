package com.acme.saf.saf_runtime.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for the SAF framework.
 * Framework component - configures generic WebSocket support for actors.
 * 
 * Registers the WebSocket endpoint: ws://host:port/ws/actors/{actorId}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ActorWebSocketHandler actorWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(actorWebSocketHandler, "/ws/actors/**")
                .setAllowedOrigins("*");  // Configure CORS as needed
    }
}
