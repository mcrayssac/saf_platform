package com.acme.saf.actor.core;

public class DefaultActorContext implements ActorContext {

    // --- Core Fields ---
    private final ActorRef self;
    private final Logger logger;
    private final Mailbox mailbox;
    private final Actor actor;
    
    private ActorRef sender;
    private final ActorLogger actorLogger;
    private String correlationId;
    private WebSocketMessageSender webSocketSender;  // Optional
    private ActorSystem actorSystem;  // For lookups

    public DefaultActorContext(ActorRef self, Logger logger, Mailbox mailbox, Actor actor) {
        this(self, logger, mailbox, actor, NoOpActorLogger.getInstance());
    }

    public DefaultActorContext(ActorRef self, Logger logger, Mailbox mailbox, Actor actor, ActorLogger actorLogger) {
        this.self = self;
        this.logger = logger;
        this.mailbox = mailbox;
        this.actor = actor;
        this.actorLogger = actorLogger != null ? actorLogger : NoOpActorLogger.getInstance();
    }

    // Setters for runtime injection
    
    public void setSender(ActorRef sender) {
        this.sender = sender;
    }

    public void setWebSocketSender(WebSocketMessageSender sender) {
        this.webSocketSender = sender;
    }
    
    public void setActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // Getters

    @Override
    public Mailbox getMailbox() { return mailbox; }

    @Override
    public Actor getActor() { return actor; }

    @Override
    public ActorRef self() { return self; }

    @Override
    public ActorRef sender() { return sender; }

    @Override
    public ActorLogger getLogger() { return actorLogger; }

    @Override
    public String getCorrelationId() { return correlationId; }

    // Logging Implementation

    @Override
    public void logInfo(String message) { logger.info(message); }

    @Override
    public void logWarning(String message) { logger.warning(message); }

    @Override
    public void logError(String message, Throwable t) { logger.error(message, t); }

    @Override
    public void publishEvent(ActorLifecycleEvent event) {
        if (logger instanceof SimpleLogger simpleLogger) {
            simpleLogger.logEvent(event);
        } else {
            logger.info("Event: " + event);
        }
    }
    
    // WebSocket & System Support

    @Override
    public void sendToWebSocket(Object message) {
        if (webSocketSender != null) {
            webSocketSender.sendToActor(self.getActorId(), message);
        } else {
            logger.info("WebSocket not available for actor: " + self.getActorId());
        }
    }
    
    @Override
    public boolean hasWebSocketConnection() {
        return webSocketSender != null;
    }
    
    @Override
    public ActorRef actorFor(String actorId) {
        if (actorSystem != null) {
            return actorSystem.getActor(actorId);
        }
        return null;
    }
}