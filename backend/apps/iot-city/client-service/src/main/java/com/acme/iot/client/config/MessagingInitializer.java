package com.acme.iot.client.config;

import com.acme.saf.saf_runtime.messaging.InterPodMessaging;
import com.acme.saf.saf_runtime.messaging.MessagingConfiguration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the InterPodMessaging system on application startup.
 */
@Component
public class MessagingInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingInitializer.class);
    
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        try {
            // Try to get existing instance
            try {
                InterPodMessaging.getInstance();
                logger.info("InterPodMessaging already initialized");
            } catch (IllegalStateException e) {
                // Not initialized yet, initialize it
                logger.info("Initializing InterPodMessaging...");
                MessagingConfiguration config = new MessagingConfiguration();
                config.initializeMessaging();
                logger.info("InterPodMessaging initialized successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize InterPodMessaging", e);
            throw new RuntimeException("Failed to initialize InterPodMessaging", e);
        }
    }
}
