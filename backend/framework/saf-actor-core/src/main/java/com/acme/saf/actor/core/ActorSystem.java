package com.acme.saf.actor.core;

import java.util.Map;
import java.util.List;

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
    
    /**
     * Obtient le statut de santé d'un acteur.
     * @param id Identifiant de l'acteur
     * @return Statut de santé de l'acteur
     */
    ActorHealthStatus getActorHealth(String id);
    
    /**
     * Redémarre un acteur en cas d'erreur.
     * @param id Identifiant de l'acteur à redémarrer
     * @return true si le restart a réussi, false sinon
     */
    boolean restartActor(String id);
    
    /**
     * Liste tous les identifiants d'acteurs actuellement en cours d'exécution.
     * @return Liste des IDs d'acteurs
     */
    List<String> getAllActorIds();
}
