package com.acme.saf.actor.core;

public interface ActorContext {
    void logInfo(String message);
    void logWarning(String message);
    void logError(String message, Throwable t);
}
