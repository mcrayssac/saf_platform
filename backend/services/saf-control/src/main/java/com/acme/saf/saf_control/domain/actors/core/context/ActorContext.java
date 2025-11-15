package com.acme.saf.saf_control.domain.actors.core.context;

public interface ActorContext {
    void logInfo(String message);
    void logWarning(String message);
    void logError(String message, Throwable t);
}
