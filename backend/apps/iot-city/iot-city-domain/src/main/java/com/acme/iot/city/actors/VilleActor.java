package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.Message;

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
    public void preStart() {
        System.out.println("VilleActor démarré avec params: " + params);
        state = "running";
    }

    @Override
    public void receive(Message message) {
        Object payload = message.getPayload();
        System.out.println("VilleActor reçoit: " + payload);

        if (payload instanceof String msg) {
            if (msg.startsWith("QUERY:")) {
                String response = "Réponse de la ville: " + msg.substring(6);
                System.out.println(response);
            } else {
                System.out.println("Message traité par la ville: " + msg);
            }
        }
    }

    @Override
    public void postStop() {
        System.out.println("VilleActor arrêté");
        state = "stopped";
    }
}
