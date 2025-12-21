package com.acme.saf.saf_control.runtime.factory;

import com.acme.saf.saf_control.runtime.core.Actor;
import com.acme.saf.saf_control.runtime.core.ActorContext;

import java.util.Map;

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