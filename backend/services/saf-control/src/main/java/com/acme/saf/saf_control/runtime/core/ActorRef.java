package com.acme.saf.saf_control.runtime.core;

import java.util.concurrent.CompletableFuture;

/**
 * Référence vers un acteur.
 * Permet d'envoyer des messages sans connaître l'acteur directement.
 */
public interface ActorRef {

    // Identifiant unique de l'acteur.
    String getId();

    // Type de l'acteur (CLIENT, VILLE, CAPTEUR).
    String getType();

    /**
     * Envoie un message sans attendre de réponse (mode fire-and-forget).
     * @param message Le message à envoyer
     */
    void tell(Object message);

    /**
     * Envoie un message et attend une réponse (mode request-response).
     * @param message Le message à envoyer
     * @param timeoutMs Durée maximale d'attente en millisecondes
     */
    CompletableFuture<Object> ask(Object message, long timeoutMs);

    // Arrête l'acteur
    void stop();
}