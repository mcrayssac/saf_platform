package com.acme.saf.saf_control.runtime.core;

/**
 * Interface de base pour tous les acteurs.
 * Un acteur traite des messages de manière asynchrone.
 */
public interface Actor {

    /**
     * Méthode appelée quand l'acteur reçoit un message.
     * @param message Le message à traiter
     * @param context Le contexte d'exécution
     */
    void receive(Object message, ActorContext context);

    /**
     * Méthode appelée au démarrage de l'acteur.
     * @param context Le contexte d'exécution
     */
    default void preStart(ActorContext context) {
        // Comportement par défaut : ne rien faire
    }

    /**
     * Méthode appelée à l'arrêt de l'acteur.
     * @param context Le contexte d'exécution
     */
    default void postStop(ActorContext context) {
        // Comportement par défaut : ne rien faire
    }
}