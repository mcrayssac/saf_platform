package com.acme.saf.actor.core;

public class DefaultActorContext implements ActorContext {

    private final Logger logger;
    private final ActorRef self;
    private ActorRef sender;
    private final ActorLogger actorLogger;
    private String correlationId;
    private WebSocketMessageSender webSocketSender;  // Optional WebSocket support
    private ActorSystem actorSystem;  // For actor lookup

    public DefaultActorContext(ActorRef self, Logger logger) {
        this(self, logger, NoOpActorLogger.getInstance());
    }

    public DefaultActorContext(ActorRef self, Logger logger, ActorLogger actorLogger) {
        this.logger = logger;
        this.self = self;
        this.actorLogger = actorLogger != null ? actorLogger : NoOpActorLogger.getInstance();
    }
    
    /**
     * Set the WebSocket message sender (optional).
     * Called by the runtime if WebSocket support is available.
     * 
     * @param sender the WebSocket message sender
     */
    public void setWebSocketSender(WebSocketMessageSender sender) {
        this.webSocketSender = sender;
    }
    
    /**
     * Set the actor system (for actor lookup).
     * Called by the runtime during context creation.
     * 
     * @param actorSystem the actor system
     */
    public void setActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }

    @Override
    public void logWarning(String message) {
        logger.warning(message);
    }

    @Override
    public void logError(String message, Throwable t) {
        logger.error(message, t);
    }

    // Appelé par le Runtime juste avant de traiter un message
    public void setSender(ActorRef sender) {
        this.sender = sender;
    }

    @Override
    public ActorRef self() {
        return self;
    }

    @Override
    public ActorRef sender() {
        return sender;
    }

    @Override
    public ActorLogger getLogger() {
        return actorLogger;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public void publishEvent(ActorLifecycleEvent event) {
        if (logger instanceof SimpleLogger simpleLogger) {
            simpleLogger.logEvent(event);
        } else {
            logger.info("Event: " + event);
        }
    }
    
    @Override
    public void sendToWebSocket(Object message) {
        if (webSocketSender != null) {
            webSocketSender.sendToActor(self.getActorId(), message);
        } else {
            // WebSocket support not available - silently ignore
            logger.info("WebSocket not available for actor: " + self.getActorId());
        }
    }
    
    @Override
    public boolean hasWebSocketConnection() {
        // Simple check - if webSocketSender is set, assume connection capability exists
        return webSocketSender != null;
    }
    
    @Override
    public ActorRef actorFor(String actorId) {
        // Delegate to ActorSystem to look up actor
        if (actorSystem != null) {
            return actorSystem.getActor(actorId);
        }
        return null;
    }
}
