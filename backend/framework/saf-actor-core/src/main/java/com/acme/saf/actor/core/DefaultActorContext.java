package com.acme.saf.actor.core;

public class DefaultActorContext implements ActorContext {

    private final Logger logger;
    private final ActorRef self;
    private ActorRef sender;
    private final ActorLogger actorLogger;
    private String correlationId;

    public DefaultActorContext(ActorRef self, Logger logger) {
        this(self, logger, NoOpActorLogger.getInstance());
    }

    public DefaultActorContext(ActorRef self, Logger logger, ActorLogger actorLogger) {
        this.logger = logger;
        this.self = self;
        this.actorLogger = actorLogger != null ? actorLogger : NoOpActorLogger.getInstance();
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
}
