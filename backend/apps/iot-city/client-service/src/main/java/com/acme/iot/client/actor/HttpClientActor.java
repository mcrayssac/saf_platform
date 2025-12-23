package com.acme.iot.client.actor;

import com.acme.iot.city.actors.ClientActor;

import java.util.Map;

/**
 * HttpClientActor - Wrapper extending ClientActor from iot-city-domain.
 * 
 * In microservice architecture, each actor type is deployed in its own service.
 * This class simply extends the domain ClientActor and runs in the client-service.
 * 
 * Communication flow:
 * 1. Frontend → saf-control
 * 2. saf-control → client-service (creates HttpClientActor via /runtime/create-actor)
 * 3. client-service → ville-service (via RegisterClient message through saf-control)
 * 4. ville-service → client-service (ClimateReport via /runtime/tell through saf-control)
 * 5. client-service → Frontend (via WebSocket)
 */
public class HttpClientActor extends ClientActor {
    
    /**
     * Constructor matching ClientActor signature.
     * 
     * @param params Actor parameters including sessionId, clientId, etc.
     */
    public HttpClientActor(Map<String, Object> params) {
        super(params);
    }
}
