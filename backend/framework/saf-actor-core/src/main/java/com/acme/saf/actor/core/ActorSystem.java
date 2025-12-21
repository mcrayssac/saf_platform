package com.acme.saf.actor.core;

import java.util.Map;

/**
 * Système de gestion des acteurs.
 * Responsable de la création, supervision et cycle de vie des acteurs.
 */
public interface ActorSystem {

    /**
     * Crée et démarre un nouvel acteur.
     * @param type Type de l'acteur (ex: "CLIENT", "VILLE", "CAPTEUR")
     * @param params Paramètres de création
     * @return Référence vers l'acteur créé
     */
    ActorRef spawn(String type, Map<String, Object> params);

    /**
     * Récupère une référence vers un acteur existant.
     * @param id Identifiant de l'acteur
     * @return Référence vers l'acteur, ou null si introuvable
     */
    ActorRef getActor(String id);

    /**
     * Arrête un acteur.
     * @param id Identifiant de l'acteur à arrêter
     */
    void stop(String id);

    /**
     * Arrête tous les acteurs et ferme le système.
     */
    void shutdown();

    /**
     * Vérifie si un acteur existe.
     * @param id Identifiant de l'acteur
     * @return true si l'acteur existe
     */
    boolean hasActor(String id);
}
