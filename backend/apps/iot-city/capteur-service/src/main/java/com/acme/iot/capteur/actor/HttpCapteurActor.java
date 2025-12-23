package com.acme.iot.capteur.actor;

import com.acme.iot.city.actors.CapteurActor;
import com.acme.saf.actor.core.Message;

import java.util.Map;

/**
 * HTTP-based CapteurActor implementation for capteur-service.
 * 
 * Wraps the domain CapteurActor to make it compatible with the
 * microservice architecture where actors are created dynamically
 * via HTTP requests from saf-control.
 */
public class HttpCapteurActor extends CapteurActor {
    
    public HttpCapteurActor(Map<String, Object> params) throws Exception {
        super(params);
    }
    
    @Override
    public void receive(Message message) throws Exception {
        super.receive(message);
    }
}
