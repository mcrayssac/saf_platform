package com.acme.iot.capteur.config;

import com.acme.iot.capteur.actor.HttpCapteurActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.RemoteMessageTransport;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import com.acme.saf.saf_runtime.messaging.InterPodMessaging;
import com.acme.saf.saf_runtime.messaging.KafkaActorMessageHandler;
import com.acme.saf.saf_runtime.messaging.KafkaMessageTransport;
import com.acme.saf.saf_runtime.messaging.MessagingConfiguration;
import com.acme.saf.saf_runtime.websocket.ActorWebSocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class ActorSystemConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(ActorSystemConfiguration.class);

    @Autowired(required = false)
    private ActorWebSocketManager webSocketManager;
    
    private InterPodMessaging interPodMessaging;
    private KafkaActorMessageHandler kafkaMessageHandler;

    @Bean
    public InterPodMessaging interPodMessaging() {
        try {
            MessagingConfiguration config = new MessagingConfiguration();
            this.interPodMessaging = config.initializeMessaging();
            logger.info("InterPodMessaging initialized for capteur-service");
            return interPodMessaging;
        } catch (Exception e) {
            logger.error("Failed to initialize InterPodMessaging", e);
            throw new RuntimeException("Failed to initialize Kafka messaging", e);
        }
    }
    
    @Bean
    public RemoteMessageTransport kafkaMessageTransport(InterPodMessaging messaging) {
        KafkaMessageTransport transport = new KafkaMessageTransport(messaging);
        logger.info("KafkaMessageTransport created for capteur-service");
        return transport;
    }

    @Bean
    public ActorSystem actorSystem(HttpCapteurActorFactory actorFactory, 
                                   InterPodMessaging messaging,
                                   RemoteMessageTransport kafkaTransport) {
        DefaultActorSystem system = new DefaultActorSystem(actorFactory);
        
        // Inject WebSocket support if available
        if (webSocketManager != null) {
            system.setWebSocketSender(webSocketManager);
            logger.info("WebSocket support enabled for ActorSystem");
        } else {
            logger.info("WebSocket support not available for ActorSystem");
        }
        
        // Initialize Kafka message handler to receive inter-pod messages
        this.kafkaMessageHandler = new KafkaActorMessageHandler(system, messaging);
        kafkaMessageHandler.start();
        logger.info("KafkaActorMessageHandler started for capteur-service");
        
        return system;
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down capteur-service messaging...");
        
        if (kafkaMessageHandler != null) {
            kafkaMessageHandler.stop();
        }
        
        if (interPodMessaging != null) {
            try {
                interPodMessaging.shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down InterPodMessaging", e);
            }
        }
    }
}
