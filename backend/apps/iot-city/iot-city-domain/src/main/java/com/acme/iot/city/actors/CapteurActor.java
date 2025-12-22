package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.Message;

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
    public void preStart() {
        System.out.println("CapteurActor démarré avec params: " + params);
        state = "running";
    }

    @Override
    public void receive(Message message) {
        Object payload = message.getPayload();
        System.out.println("CapteurActor reçoit: " + payload);

        if (payload instanceof String msg) {
            if (msg.startsWith("QUERY:")) {
                String response = "Données du capteur: " + msg.substring(6);
                System.out.println(response);
            } else {
                System.out.println("Capteur enregistre: " + msg);
            }
        }
    }

    @Override
    public void postStop() {
        System.out.println("CapteurActor arrêté");
        state = "stopped";
    }
}
