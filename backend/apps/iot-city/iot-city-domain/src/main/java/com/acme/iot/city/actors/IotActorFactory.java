package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;

import java.util.Map;

/**
 * Factory spécifique à l'application iot-city.
 * Crée les acteurs métier : Client, Ville, Capteur.
 * 
 * Cette factory sera injectée dans le runtime SAF pour permettre
 * la création d'acteurs métier sans que le framework connaisse ces types.
 */
public class IotActorFactory implements ActorFactory {

    @Override
    public Actor create(String type, Map<String, Object> params) {
        return switch (type.toUpperCase()) {
            case "CLIENT" -> new ClientActor(params);
            case "VILLE" -> new VilleActor(params);
            case "CAPTEUR" -> new CapteurActor(params);
            default -> null;
        };
    }

    @Override
    public boolean supports(String type) {
        return "CLIENT".equalsIgnoreCase(type)
                || "VILLE".equalsIgnoreCase(type)
                || "CAPTEUR".equalsIgnoreCase(type);
    }
}
