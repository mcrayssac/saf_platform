package com.acme.iot.ville.actor;

import com.acme.iot.city.actors.VilleActor;

import java.util.Map;

/**
 * HttpVilleActor - Wrapper extending VilleActor from iot-city-domain.
 * 
 * In microservice architecture, each actor type is deployed in its own service.
 * This class simply extends the domain VilleActor and runs in the ville-service.
 * 
 * Communication flow:
 * 1. Frontend/client → saf-control
 * 2. saf-control → ville-service (creates HttpVilleActor via /runtime/create-actor)
 * 3. ville-service receives RegisterClient from client-service
 * 4. ville-service receives sensor data from capteur-service
 * 5. ville-service → client-service (ClimateReport via /runtime/tell through saf-control)
 */
public class HttpVilleActor extends VilleActor {
    
    /**
     * Constructor matching VilleActor signature.
     * 
     * @param params Actor parameters including villeName
     */
    public HttpVilleActor(Map<String, Object> params) {
        super(params);
    }
}
