package com.acme.saf.actor.core;

public interface ActorContext {
    // Identification
    ActorRef self();
    ActorRef sender();

    // Logging
    void logInfo(String message);
    void logWarning(String message);
    void logError(String message, Throwable t);

    // Publication d'évenements
    void publishEvent(ActorLifecycleEvent event);
}
