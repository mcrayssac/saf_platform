package com.acme.saf.saf_runtime.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestionnaire centralisé pour la communication inter-pods.
 * 
 * Fournit une interface unifiée pour accéder au producteur et au consommateur de messages,
 * permettant aux acteurs de communiquer facilement avec d'autres pods.
 */
public class InterPodMessaging {
    
    private static final Logger logger = LoggerFactory.getLogger(InterPodMessaging.class);
    
    private static volatile InterPodMessaging instance;
    
    private final MessageProducer producer;
    private final MessageConsumer consumer;
    
    private InterPodMessaging(MessageProducer producer, MessageConsumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }
    
    /**
     * Initialise le gestionnaire avec un producteur et un consommateur.
     * 
     * @param producer Le producteur de messages
     * @param consumer Le consommateur de messages
     * @return L'instance du gestionnaire
     */
    public static InterPodMessaging initialize(MessageProducer producer, MessageConsumer consumer) {
        if (instance != null) {
            logger.warn("InterPodMessaging already initialized, reinitializing");
            try {
                instance.shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down previous instance", e);
            }
        }
        
        instance = new InterPodMessaging(producer, consumer);
        logger.info("InterPodMessaging initialized");
        return instance;
    }
    
    /**
     * Retourne l'instance unique du gestionnaire.
     * 
     * @return L'instance ou null si non initialisée
     */
    public static InterPodMessaging getInstance() {
        if (instance == null) {
            throw new IllegalStateException("InterPodMessaging not initialized. Call initialize() first.");
        }
        return instance;
    }
    
    /**
     * Retourne le producteur de messages.
     * 
     * @return Le producteur
     */
    public MessageProducer getProducer() {
        return producer;
    }
    
    /**
     * Retourne le consommateur de messages.
     * 
     * @return Le consommateur
     */
    public MessageConsumer getConsumer() {
        return consumer;
    }
    
    /**
     * Vérifie si le gestionnaire est connecté au broker.
     * 
     * @return true si connecté, false sinon
     */
    public boolean isConnected() {
        return producer.isConnected() && consumer.isConnected();
    }
    
    /**
     * Arrête le gestionnaire et ferme les connexions.
     * 
     * @throws Exception en cas d'erreur
     */
    public void shutdown() throws Exception {
        logger.info("Shutting down InterPodMessaging");
        try {
            producer.close();
            consumer.close();
            instance = null;
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
            throw e;
        }
    }
}
