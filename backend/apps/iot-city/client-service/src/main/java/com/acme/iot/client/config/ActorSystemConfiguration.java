package com.acme.iot.client.config;

import com.acme.iot.client.actor.HttpClientActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import com.acme.saf.saf_runtime.websocket.ActorWebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfiguration {

    @Autowired(required = false)
    private ActorWebSocketManager webSocketManager;

    @Bean
    public ActorSystem actorSystem(HttpClientActorFactory actorFactory) {
        DefaultActorSystem system = new DefaultActorSystem(actorFactory);
        
        // Inject WebSocket support if available
        if (webSocketManager != null) {
            system.setWebSocketSender(webSocketManager);
            System.out.println("WebSocket support enabled for ActorSystem");
        } else {
            System.out.println("WebSocket support not available for ActorSystem");
        }
        
        return system;
    }
}
