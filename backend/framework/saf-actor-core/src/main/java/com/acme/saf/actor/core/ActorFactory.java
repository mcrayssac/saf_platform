package com.acme.saf.actor.core;

import java.util.Map;

/**
 * Interface de plugin pour créer des acteurs personnalisés.
 * Permet d'injecter des types d'acteurs sans que le Runtime
 * ne dépende de ces types.
 */
public interface ActorFactory {

    /**
     * Crée une instance d'acteur pour un type donné.
     * @param type Type de l'acteur (ex: "CLIENT", "VILLE", "CAPTEUR")
     * @param params Paramètres de création
     * @return Instance de l'acteur, ou null si le type n'est pas supporté
     */
    Actor create(String type, Map<String, Object> params);

    /**
     * Vérifie si cette factory supporte un type donné.
     * @param type Type de l'acteur
     * @return true si ce type est supporté
     */
    boolean supports(String type);
}
