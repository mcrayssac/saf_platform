package com.acme.saf.saf_runtime.messaging;

import java.util.function.Consumer;

/**
 * Consommateur pour recevoir les messages du broker et les dispatcher aux acteurs.
 * 
 * Permet aux acteurs de recevoir les messages provenant d'autres pods.
 * Gère la désérialisation automatique des BrokerMessages vers les messages applicatifs.
 */
public interface MessageConsumer {
    
    /**
     * Enregistre un listener pour un type de message donné.
     * 
     * @param messageType Le type de message (FQCN de la classe)
     * @param messageClass La classe du message
     * @param listener Le listener à appeler
     * @param <T> Le type du message
     */
    <T> void subscribe(String messageType, Class<T> messageClass, Consumer<T> listener);
    
    /**
     * Enregistre un listener pour un type de message avec gestion d'erreur.
     * 
     * @param messageType Le type de message
     * @param messageClass La classe du message
     * @param listener Le listener à appeler
     * @param errorHandler Handler pour les erreurs
     * @param <T> Le type du message
     */
    <T> void subscribe(String messageType, Class<T> messageClass, Consumer<T> listener,
            Consumer<Exception> errorHandler);
    
    /**
     * Démarre l'écoute des messages sur un topic.
     * 
     * @param topic Le topic à écouter
     * @throws Exception en cas d'erreur
     */
    void listen(String topic) throws Exception;
    
    /**
     * Arrête l'écoute d'un topic.
     * 
     * @param topic Le topic
     * @throws Exception en cas d'erreur
     */
    void stopListening(String topic) throws Exception;
    
    /**
     * Vérifie si le consommateur est connecté au broker.
     * 
     * @return true si connecté, false sinon
     */
    boolean isConnected();
    
    /**
     * Ferme le consommateur.
     * 
     * @throws Exception en cas d'erreur de fermeture
     */
    void close() throws Exception;
}
