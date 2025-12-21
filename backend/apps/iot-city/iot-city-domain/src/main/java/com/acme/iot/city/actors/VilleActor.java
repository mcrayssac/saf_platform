package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorContext;

import java.util.Map;

/**
 * Acteur métier représentant une Ville dans le système IoT.
 * Spécifique à l'application iot-city.
 */
public class VilleActor implements Actor {

    private final Map<String, Object> params;
    private String state = "initialized";

    public VilleActor(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void preStart(ActorContext context) {
        System.out.println("VilleActor démarré avec params: " + params);
        state = "running";
    }

    @Override
    public void receive(Object message, ActorContext context) {
        System.out.println("VilleActor reçoit: " + message);

        if (message instanceof String msg) {
            if (msg.startsWith("QUERY:")) {
                String response = "Réponse de la ville: " + msg.substring(6);
                context.reply(response);
            } else {
                System.out.println("Message traité par la ville: " + msg);
            }
        }
    }

    @Override
    public void postStop(ActorContext context) {
        System.out.println("VilleActor arrêté");
        state = "stopped";
    }
}
