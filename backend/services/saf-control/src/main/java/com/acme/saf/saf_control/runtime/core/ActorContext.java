package com.acme.saf.saf_control.runtime.core;

/**
 * Contexte d'exécution fourni à un acteur.
 * Contient les informations et capacités disponibles.
 */
public interface ActorContext {

    // Référence vers l'acteur lui-même
    ActorRef getSelf();

    // Référence vers l'acteur qui a envoyé le message courant
    ActorRef getSender();

    // Système d'acteurs parent
    ActorSystem getSystem();

    /**
     * Envoie une réponse à l'expéditeur du message courant.
     * Utilisé dans le mode "ask" pour retourner une valeur.
     */
    void reply(Object response);
}