package com.acme.saf.saf_control.domain.actors.core.context;

import com.acme.saf.saf_control.domain.actors.core.logging.Logger;

public class DefaultActorContext implements ActorContext {

    private final Logger logger;

    public DefaultActorContext(Logger logger) {
        this.logger = logger;
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
}


