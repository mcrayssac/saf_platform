package com.acme.saf.saf_runtime.messaging;

import com.acme.saf.actor.core.ActorLifecycleState;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Message;
import com.acme.saf.actor.core.RemoteMessageTransport;
import com.acme.saf.actor.core.SimpleMessage;
import com.acme.saf.actor.core.protocol.TellActorCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka-based implementation of RemoteMessageTransport.
 * 
 * Uses Kafka topics to send messages between actors across different pods/services.
 * Topic naming convention: "actor-messages-{targetActorId}" or "actor-messages-{serviceId}"
 * 
 * This replaces the HTTP-based transport for inter-pod actor communication.
 */
public class KafkaMessageTransport implements RemoteMessageTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageTransport.class);
    
    // Topic prefix for actor messages
    private static final String ACTOR_TOPIC_PREFIX = "actor-messages";
    
    private final InterPodMessaging messaging;
    
    /**
     * Creates a new KafkaMessageTransport using the provided messaging system.
     * 
     * @param messaging the InterPodMessaging instance
     */
    public KafkaMessageTransport(InterPodMessaging messaging) {
        if (messaging == null) {
            throw new IllegalArgumentException("InterPodMessaging cannot be null");
        }
        this.messaging = messaging;
        logger.info("KafkaMessageTransport initialized");
    }
    
    /**
     * Creates a new KafkaMessageTransport using the singleton InterPodMessaging instance.
     */
    public KafkaMessageTransport() {
        this(InterPodMessaging.getInstance());
    }
    
    /**
     * Derives the Kafka topic name from an actor ID or URL.
     * 
     * For local actor IDs: uses a shared topic "actor-messages"
     * For URLs containing service info: could route to service-specific topics
     * 
     * @param actorIdOrUrl the actor ID or URL
     * @return the Kafka topic name
     */
    private String deriveTopicFromActorId(String actorIdOrUrl) {
        // For simplicity, we use a single shared topic for all actor messages
        // Messages contain the targetActorId, so the consumer can route appropriately
        // In a more complex setup, we could use service-specific topics
        return ACTOR_TOPIC_PREFIX;
    }
    
    @Override
    public void sendMessage(String actorIdOrUrl, Message message, ActorRef sender) throws Exception {
        if (actorIdOrUrl == null || actorIdOrUrl.isEmpty()) {
            throw new IllegalArgumentException("Actor ID/URL cannot be null or empty");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (!messaging.isConnected()) {
            throw new Exception("Kafka messaging is not connected");
        }
        
        try {
            // Extract actorId from URL if necessary (e.g., "http://service:8080/actors/id" -> "id")
            String targetActorId = extractActorId(actorIdOrUrl);
            String senderActorId = sender != null ? sender.getActorId() : "";
            
            // Wrap the message in a TellActorCommand for proper routing
            Message wrappedMessage = message;
            if (!(message instanceof SimpleMessage)) {
                wrappedMessage = new SimpleMessage(message.getPayload());
            }
            
            TellActorCommand command = new TellActorCommand(targetActorId, senderActorId, wrappedMessage);
            
            // Derive the topic and send
            String topic = deriveTopicFromActorId(targetActorId);
            
            messaging.getProducer().send(command, topic, TellActorCommand.class.getName());
            
            logger.debug("Kafka message sent to actor {} via topic {}", targetActorId, topic);
            
        } catch (Exception e) {
            logger.error("Failed to send message via Kafka to actor: {}", actorIdOrUrl, e);
            throw e;
        }
    }
    
    /**
     * Extracts the actor ID from a URL or returns the input if it's already an ID.
     * 
     * Examples:
     * - "http://ville-service:8085/actors/ville-123" -> "ville-123"
     * - "ville-123" -> "ville-123"
     * 
     * @param actorIdOrUrl the actor ID or URL
     * @return the extracted actor ID
     */
    private String extractActorId(String actorIdOrUrl) {
        if (actorIdOrUrl == null) {
            return null;
        }
        
        // If it contains "/actors/", extract the ID
        if (actorIdOrUrl.contains("/actors/")) {
            int idx = actorIdOrUrl.lastIndexOf("/actors/");
            String afterActors = actorIdOrUrl.substring(idx + 8);
            // Remove any trailing path or query string
            int slashIdx = afterActors.indexOf('/');
            if (slashIdx > 0) {
                afterActors = afterActors.substring(0, slashIdx);
            }
            int queryIdx = afterActors.indexOf('?');
            if (queryIdx > 0) {
                afterActors = afterActors.substring(0, queryIdx);
            }
            return afterActors;
        }
        
        // Otherwise, assume it's already an actor ID
        return actorIdOrUrl;
    }
    
    @Override
    public CompletableFuture<Object> askMessage(String actorUrl, Message message, long timeout, TimeUnit unit) throws Exception {
        // Request-reply pattern is more complex with Kafka
        // For now, we can implement a correlation-based approach or throw UnsupportedOperationException
        // A simple implementation: send the message and use a reply topic
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        
        // For now, mark this as not fully implemented
        // The ask pattern requires a reply mechanism which is more complex
        logger.warn("Ask pattern via Kafka is not fully implemented yet");
        future.completeExceptionally(new UnsupportedOperationException(
            "Ask pattern not yet fully supported for Kafka transport. Use tell pattern instead."));
        
        return future;
    }
    
    @Override
    public boolean checkActorExists(String actorUrl) throws Exception {
        // With Kafka, we can't directly check if an actor exists
        // This would require a request-reply pattern or a separate registry check
        logger.debug("checkActorExists not directly supported via Kafka, returning true by default");
        return true;
    }
    
    @Override
    public void stopActor(String actorUrl) throws Exception {
        // Stopping a remote actor via Kafka would require sending a special "stop" message
        logger.warn("stopActor via Kafka not implemented - actors should be stopped locally");
        throw new UnsupportedOperationException("Cannot stop remote actors directly via Kafka");
    }
    
    @Override
    public ActorLifecycleState getActorState(String actorUrl) throws Exception {
        // Getting actor state requires a request-reply pattern
        logger.debug("getActorState not directly supported via Kafka");
        return ActorLifecycleState.RUNNING; // Assume running
    }
    
    /**
     * Check if the transport is connected and ready to send messages.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return messaging != null && messaging.isConnected();
    }
}
