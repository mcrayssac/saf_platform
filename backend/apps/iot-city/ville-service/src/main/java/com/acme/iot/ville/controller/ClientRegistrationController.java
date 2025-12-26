package com.acme.iot.ville.controller;

import com.acme.iot.city.actors.VilleActor;
import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.ville.messaging.ClientSubscription;
import com.acme.saf.saf_runtime.messaging.InterPodMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API for client registration/unregistration with cities.
 * 
 * When a client registers:
 * 1. Creates a Kafka subscription to the city's weather topic
 * 2. Caches the client's subscription for future queries
 * 
 * When a client unregisters:
 * 1. Closes the Kafka subscription
 * 2. Removes the cached subscription
 */
@Slf4j
@RestController
@RequestMapping("/registration")
public class ClientRegistrationController {
    
    // Cache active subscriptions: key = "villeId:clientId"
    private static final Map<String, ClientSubscription> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * Register a client with a city (subscribe to Kafka weather topic).
     * POST /registration/register?villeId=paris&clientId=alice
     */
    @PostMapping("/register")
    public Map<String, Object> registerClient(@RequestParam String villeId, @RequestParam String clientId) {
        Map<String, Object> response = new HashMap<>();
        String subscriptionKey = villeId + ":" + clientId;
        
        try {
            VilleActor villeActor = WeatherController.villeActorCache.get(villeId);
            
            if (villeActor == null) {
                response.put("status", "ERROR");
                response.put("error", "City not found: " + villeId);
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                return response;
            }
            
            // Check if already subscribed
            if (activeSubscriptions.containsKey(subscriptionKey)) {
                response.put("status", "ERROR");
                response.put("error", "Client already registered to this city");
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                return response;
            }
            
            // Get InterPodMessaging for Kafka subscription
            InterPodMessaging messaging = InterPodMessaging.getInstance();
            
            // Create Kafka subscription to the city's weather topic
            ClientSubscription subscription = new ClientSubscription(clientId, villeId, messaging);
            activeSubscriptions.put(subscriptionKey, subscription);
            
            log.info("Client {} subscribed to city {} via Kafka topic: ville-{}-weather", 
                clientId, villeId, villeId);
            
            response.put("status", "SUCCESS");
            response.put("message", "Client registered and subscribed to weather topic");
            response.put("villeId", villeId);
            response.put("clientId", clientId);
            response.put("villeName", villeActor.getName());
            response.put("topic", "ville-" + villeId + "-weather");
            
        } catch (Exception e) {
            log.error("Failed to register client: " + clientId + " with city: " + villeId, e);
            response.put("status", "ERROR");
            response.put("error", "Failed to register client: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            response.put("villeId", villeId);
            response.put("clientId", clientId);
        }
        
        return response;
    }

    /**
     * Unregister a client from a city (unsubscribe from Kafka weather topic).
     * POST /registration/unregister?villeId=paris&clientId=alice
     */
    @PostMapping("/unregister")
    public Map<String, Object> unregisterClient(@RequestParam String villeId, @RequestParam String clientId) {
        Map<String, Object> response = new HashMap<>();
        String subscriptionKey = villeId + ":" + clientId;
        
        try {
            VilleActor villeActor = WeatherController.villeActorCache.get(villeId);
            
            if (villeActor == null) {
                response.put("status", "ERROR");
                response.put("error", "City not found: " + villeId);
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                return response;
            }
            
            // Get and close the subscription
            ClientSubscription subscription = activeSubscriptions.remove(subscriptionKey);
            
            if (subscription == null) {
                response.put("status", "ERROR");
                response.put("error", "Client not registered to this city");
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                return response;
            }
            
            // Close the Kafka subscription
            subscription.close();
            
            log.info("Client {} unsubscribed from city {} (Kafka topic: ville-{}-weather)", 
                clientId, villeId, villeId);
            
            response.put("status", "SUCCESS");
            response.put("message", "Client unregistered and unsubscribed from weather topic");
            response.put("villeId", villeId);
            response.put("clientId", clientId);
            response.put("villeName", villeActor.getName());
            
        } catch (Exception e) {
            log.error("Failed to unregister client: " + clientId + " from city: " + villeId, e);
            response.put("status", "ERROR");
            response.put("error", "Failed to unregister client: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            response.put("villeId", villeId);
            response.put("clientId", clientId);
        }
        
        return response;
    }

    /**
     * Get active subscriptions for a city.
     * GET /registration/clients?villeId=paris
     */
    @GetMapping("/clients")
    public Map<String, Object> getRegisteredClients(@RequestParam String villeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            VilleActor villeActor = WeatherController.villeActorCache.get(villeId);
            
            if (villeActor == null) {
                response.put("status", "ERROR");
                response.put("error", "City not found: " + villeId);
                response.put("villeId", villeId);
                response.put("clientCount", 0);
                return response;
            }
            
            // Count active subscriptions for this city
            int clientCount = (int) activeSubscriptions.keySet().stream()
                .filter(key -> key.startsWith(villeId + ":"))
                .count();
            
            response.put("status", "SUCCESS");
            response.put("villeId", villeId);
            response.put("villeName", villeActor.getName());
            response.put("clientCount", clientCount);
            
        } catch (Exception e) {
            log.error("Failed to get registered clients for city: " + villeId, e);
            response.put("status", "ERROR");
            response.put("error", "Failed to get registered clients: " + e.getMessage());
            response.put("villeId", villeId);
        }
        
        return response;
    }

    /**
     * Get the latest weather data received by a subscribed client.
     * GET /registration/get-subscription?villeId=paris&clientId=alice
     */
    @GetMapping("/get-subscription")
    public Map<String, Object> getClientSubscriptionData(@RequestParam String villeId, @RequestParam String clientId) {
        Map<String, Object> response = new HashMap<>();
        String subscriptionKey = villeId + ":" + clientId;
        
        try {
            ClientSubscription subscription = activeSubscriptions.get(subscriptionKey);
            
            if (subscription == null) {
                response.put("status", "ERROR");
                response.put("error", "Client not registered to this city");
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                response.put("hasData", false);
                return response;
            }
            
            if (subscription.getLatestReport() == null) {
                response.put("status", "SUCCESS");
                response.put("message", "Client subscribed but no data received yet");
                response.put("villeId", villeId);
                response.put("clientId", clientId);
                response.put("hasData", false);
                return response;
            }
            
            // Client has received data
            response.put("status", "SUCCESS");
            response.put("villeId", villeId);
            response.put("clientId", clientId);
            response.put("villeName", subscription.getLatestReport().getVilleName());
            response.put("hasData", true);
            response.put("timestamp", subscription.getLatestReport().getTimestamp());
            response.put("sensorCount", subscription.getLatestReport().getActiveCapteurs());
            response.put("weatherData", subscription.getLatestReport().getAggregatedData());
            
        } catch (Exception e) {
            log.error("Failed to get subscription data for client: {} from city: {}", clientId, villeId, e);
            response.put("status", "ERROR");
            response.put("error", "Failed to get subscription data: " + e.getMessage());
            response.put("villeId", villeId);
            response.put("clientId", clientId);
        }
        
        return response;
    }
}
