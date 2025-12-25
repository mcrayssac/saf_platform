package com.acme.saf.saf_runtime.messaging;

/**
 * Interface pour la sérialisation/désérialisation des messages applicatifs
 * en messages de transport (BrokerMessage).
 * 
 * Permet une conversion générique et extensible de tout type de message
 * vers un format transportable via un broker.
 */
public interface MessageSerializer {
    
    /**
     * Sérialise un message applicatif en BrokerMessage.
     * 
     * @param message L'objet message à sérialiser
     * @param destinationTopic Le topic de destination
     * @return Le message de transport sérialisé
     * @throws Exception en cas d'erreur de sérialisation
     */
    BrokerMessage serialize(Object message, String destinationTopic) 
            throws Exception;

    /**
     * Désérialise un BrokerMessage en message applicatif.
     * 
     * @param brokerMessage Le message de transport à désérialiser
     * @param targetClass La classe cible du message applicatif
     * @param <T> Le type générique du message
     * @return L'objet message désérialisé
     * @throws Exception en cas d'erreur de désérialisation
     */
    <T> T deserialize(BrokerMessage brokerMessage, Class<T> targetClass) 
            throws Exception;
    
    /**
     * Enregistre un type de message et sa classe correspondante.
     * 
     * @param messageType Le type du message (FQCN)
     * @param messageClass La classe du message
     */
    void registerMessageType(String messageType, Class<?> messageClass);
    
    /**
     * Récupère la classe enregistrée pour un type de message.
     * 
     * @param messageType Le type du message
     * @return La classe du message, ou null si non enregistrée
     */
    Class<?> getMessageClass(String messageType);
}
