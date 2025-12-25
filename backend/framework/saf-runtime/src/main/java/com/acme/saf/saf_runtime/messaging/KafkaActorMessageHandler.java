package com.acme.saf.saf_runtime.messaging;

import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.Message;
import com.acme.saf.actor.core.SimpleMessage;
import com.acme.saf.actor.core.protocol.TellActorCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming Kafka messages and dispatches them to local actors.
 * 
 * This class subscribes to the "actor-messages" topic and routes messages
 * to the appropriate actors based on the targetActorId in the TellActorCommand.
 */
public class KafkaActorMessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaActorMessageHandler.class);
    
    private static final String ACTOR_MESSAGES_TOPIC = "actor-messages";
    
    private final ActorSystem actorSystem;
    private final InterPodMessaging messaging;
    private volatile boolean running = false;
    
    /**
     * Creates a new handler for the given actor system.
     * 
     * @param actorSystem the local actor system
     * @param messaging the InterPodMessaging instance
     */
    public KafkaActorMessageHandler(ActorSystem actorSystem, InterPodMessaging messaging) {
        if (actorSystem == null) {
            throw new IllegalArgumentException("ActorSystem cannot be null");
        }
        if (messaging == null) {
            throw new IllegalArgumentException("InterPodMessaging cannot be null");
        }
        this.actorSystem = actorSystem;
        this.messaging = messaging;
    }
    
    /**
     * Starts listening for messages on the actor-messages topic.
     * Registers a subscriber for TellActorCommand messages.
     */
    public void start() {
        if (running) {
            logger.warn("KafkaActorMessageHandler is already running");
            return;
        }
        
        try {
            // Subscribe to TellActorCommand messages
            messaging.getConsumer().subscribe(
                TellActorCommand.class.getName(),
                TellActorCommand.class,
                this::handleTellActorCommand,
                this::handleError
            );
            
            // Start listening on the topic
            messaging.getConsumer().listen(ACTOR_MESSAGES_TOPIC);
            
            running = true;
            logger.info("KafkaActorMessageHandler started listening on topic: {}", ACTOR_MESSAGES_TOPIC);
            
        } catch (Exception e) {
            logger.error("Failed to start KafkaActorMessageHandler", e);
            throw new RuntimeException("Failed to start Kafka message handler", e);
        }
    }
    
    /**
     * Handles an incoming TellActorCommand by dispatching it to the target actor.
     * 
     * @param command the command received from Kafka
     */
    private void handleTellActorCommand(TellActorCommand command) {
        if (command == null) {
            logger.warn("Received null TellActorCommand");
            return;
        }
        
        String targetActorId = command.getTargetActorId();
        String senderActorId = command.getSenderActorId();
        Message message = command.getMessage();
        
        if (targetActorId == null || targetActorId.isEmpty()) {
            logger.warn("TellActorCommand has no targetActorId");
            return;
        }
        
        // Check if the actor exists locally
        ActorRef targetActor = actorSystem.getActor(targetActorId);
        
        if (targetActor == null) {
            // Actor not found locally - this message was not for us
            logger.debug("Actor {} not found locally, ignoring Kafka message", targetActorId);
            return;
        }
        
        // Dispatch the message to the local actor
        try {
            logger.info("Dispatching Kafka message to local actor: {}", targetActorId);
            
            // Create a sender reference if provided
            ActorRef senderRef = null;
            if (senderActorId != null && !senderActorId.isEmpty()) {
                // Try to find the sender locally (might be a remote actor)
                senderRef = actorSystem.getActor(senderActorId);
                // If not found locally, we could create a RemoteActorRefProxy here
                // but for simplicity, we'll just pass null or the ID as a reference
            }
            
            if (senderRef != null) {
                targetActor.tell(message, senderRef);
            } else {
                targetActor.tell(message);
            }
            
            logger.debug("Message delivered to actor {} from Kafka", targetActorId);
            
        } catch (Exception e) {
            logger.error("Failed to dispatch Kafka message to actor {}", targetActorId, e);
        }
    }
    
    /**
     * Handles errors during message processing.
     * 
     * @param error the error that occurred
     */
    private void handleError(Exception error) {
        logger.error("Error processing Kafka message", error);
    }
    
    /**
     * Stops listening for messages.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        try {
            messaging.getConsumer().stopListening(ACTOR_MESSAGES_TOPIC);
            running = false;
            logger.info("KafkaActorMessageHandler stopped");
        } catch (Exception e) {
            logger.error("Error stopping KafkaActorMessageHandler", e);
        }
    }
    
    /**
     * Check if the handler is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
}
