package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorContext;

import java.util.Map;

/**
 * Acteur métier représentant un Capteur dans le système IoT.
 * Spécifique à l'application iot-city.
 */
public class CapteurActor implements Actor {

    private final Map<String, Object> params;
    private String state = "initialized";

    public CapteurActor(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void preStart(ActorContext context) {
        System.out.println("CapteurActor démarré avec params: " + params);
        state = "running";
    }

    @Override
    public void receive(Object message, ActorContext context) {
        System.out.println("CapteurActor reçoit: " + message);

        if (message instanceof String msg) {
            if (msg.startsWith("QUERY:")) {
                String response = "Données du capteur: " + msg.substring(6);
                context.reply(response);
            } else {
                System.out.println("Capteur enregistre: " + msg);
            }
        }
    }

    @Override
    public void postStop(ActorContext context) {
        System.out.println("CapteurActor arrêté");
        state = "stopped";
    }
}
