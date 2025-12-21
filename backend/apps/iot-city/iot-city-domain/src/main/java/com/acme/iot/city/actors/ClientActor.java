package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorContext;

import java.util.Map;

/**
 * Acteur métier représentant un Client dans le système IoT.
 * Spécifique à l'application iot-city.
 */
public class ClientActor implements Actor {

    private final Map<String, Object> params;
    private String state = "initialized";

    public ClientActor(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void preStart(ActorContext context) {
        System.out.println("ClientActor démarré avec params: " + params);
        state = "running";
    }

    @Override
    public void receive(Object message, ActorContext context) {
        System.out.println("ClientActor reçoit: " + message);

        if (message instanceof String msg) {
            if (msg.startsWith("QUERY:")) {
                String response = "Réponse du client: " + msg.substring(6);
                context.reply(response);
            } else {
                System.out.println("Message traité: " + msg);
            }
        }
    }

    @Override
    public void postStop(ActorContext context) {
        System.out.println("ClientActor arrêté");
        state = "stopped";
    }
}
