package com.acme.saf.saf_runtime.messaging;

/**
 * Producteur pour publier les messages des acteurs vers un broker externe.
 * 
 * Permet aux acteurs de communiquer avec d'autres pods via un broker de messages (Kafka/RabbitMQ).
 * Gère la sérialisation des messages applicatifs en BrokerMessages.
 */
public interface MessageProducer {
    
    /**
     * Envoie un message applicatif vers le broker.
     * 
     * @param message Le message applicatif à envoyer
     * @param destinationTopic Le topic de destination sur le broker
     * @throws Exception en cas d'erreur d'envoi
     */
    void send(Object message, String destinationTopic) throws Exception;
    
    /**
     * Envoie un message applicatif avec métadonnées personnalisées.
     * 
     * @param message Le message applicatif à envoyer
     * @param destinationTopic Le topic de destination
     * @param messageType Type explicite du message (override de la classe)
     * @throws Exception en cas d'erreur d'envoi
     */
    void send(Object message, String destinationTopic, String messageType) throws Exception;
    
    /**
     * Envoie un message de manière asynchrone.
     * 
     * @param message Le message à envoyer
     * @param destinationTopic Le topic de destination
     */
    void sendAsync(Object message, String destinationTopic);
    
    /**
     * Vérifie si le producteur est connecté au broker.
     * 
     * @return true si connecté, false sinon
     */
    boolean isConnected();
    
    /**
     * Ferme le producteur.
     * 
     * @throws Exception en cas d'erreur de fermeture
     */
    void close() throws Exception;
}
