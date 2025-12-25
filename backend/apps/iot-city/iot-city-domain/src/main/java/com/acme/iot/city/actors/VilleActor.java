package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.RequestVilleInfo;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.city.messages.VilleInfoResponse;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.ClimateReport;
import com.acme.iot.city.model.SensorReading;
import com.acme.iot.city.model.VilleInfo;
import com.acme.saf.actor.core.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a city in the IoT monitoring system.
 * 
 * Responsibilities:
 * - Maintain climate configuration
 * - Track registered clients (who entered this city)
 * - Collect sensor readings from associated capteurs
 * - Aggregate data and broadcast climate reports every 5 seconds
 */
public class VilleActor implements Actor {
    
    // Configuration
    private final String name;
    private final ClimateConfig climateConfig;
    private String status = "ACTIVE";
    
    // Runtime state
    private final Set<ActorRef> registeredClients = ConcurrentHashMap.newKeySet();
    private final Map<String, SensorReading> latestReadings = new ConcurrentHashMap<>();
    
    // Scheduler for periodic climate reports
    private ScheduledExecutorService scheduler;
    private ActorContext context;
    
    public VilleActor(Map<String, Object> params) {
        this.name = (String) params.getOrDefault("nom", "UnknownCity");
        this.climateConfig = parseClimateConfig(params.get("climateConfig"));
    }
    
    /**
     * Convert a Map to ClimateConfig object.
     * Handles JSON deserialization from REST API.
     */
    @SuppressWarnings("unchecked")
    private ClimateConfig parseClimateConfig(Object configObj) {
        if (configObj == null) {
            // Default config
            return new ClimateConfig(20.0, 1013.0, 60.0, 10.0);
        }
        
        if (configObj instanceof ClimateConfig) {
            return (ClimateConfig) configObj;
        }
        
        if (configObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) configObj;
            
            // Try to parse from the map
            double meanTemp = getDoubleValue(map, "meanTemperature", 20.0);
            double meanPressure = getDoubleValue(map, "meanPressure", 1013.0);
            double meanHumidity = getDoubleValue(map, "meanHumidity", 60.0);
            double variance = getDoubleValue(map, "variancePercentage", 10.0);
            
            // Also support tempMin/tempMax format (calculate mean)
            if (map.containsKey("tempMin") && map.containsKey("tempMax")) {
                double tempMin = getDoubleValue(map, "tempMin", 0.0);
                double tempMax = getDoubleValue(map, "tempMax", 30.0);
                meanTemp = (tempMin + tempMax) / 2.0;
            }
            
            if (map.containsKey("pressureMin") && map.containsKey("pressureMax")) {
                double pressureMin = getDoubleValue(map, "pressureMin", 980.0);
                double pressureMax = getDoubleValue(map, "pressureMax", 1040.0);
                meanPressure = (pressureMin + pressureMax) / 2.0;
            }
            
            if (map.containsKey("humidityMin") && map.containsKey("humidityMax")) {
                double humidityMin = getDoubleValue(map, "humidityMin", 30.0);
                double humidityMax = getDoubleValue(map, "humidityMax", 90.0);
                meanHumidity = (humidityMin + humidityMax) / 2.0;
            }
            
            return new ClimateConfig(meanTemp, meanPressure, meanHumidity, variance);
        }
        
        // Default fallback
        return new ClimateConfig(20.0, 1013.0, 60.0, 10.0);
    }
    
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Sets the actor context. Called by framework after actor creation.
     */
    public void setContext(ActorContext context) {
        this.context = context;
    }
    
    @Override
    public void preStart() {
        System.out.println("VilleActor started: " + name);
        
        // Start periodic climate report broadcasting (every 5 seconds)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::broadcastClimateReport, 5, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public void receive(Message message) throws Exception {
        Object payload = message.getPayload();
        
        // Unwrap SimpleMessage if needed (for remote messages)
        if (payload instanceof SimpleMessage simpleMsg) {
            payload = simpleMsg.getPayload();
        }
        
        // Handle Map (from JSON deserialization) - convert to proper message objects
        if (payload instanceof Map) {
            payload = convertMapToMessage((Map<?, ?>) payload);
        }
        
        if (payload instanceof RegisterClient req) {
            handleRegisterClient(req);
        }
        else if (payload instanceof UnregisterClient req) {
            handleUnregisterClient(req);
        }
        else if (payload instanceof CapteurDataUpdate update) {
            handleCapteurDataUpdate(update);
        }
        else if (payload instanceof RequestVilleInfo req) {
            handleRequestVilleInfo(req);
        }
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("VilleActor received unknown message: " + payload.getClass().getName());
        }
    }
    
    /**
     * Convert a Map (from JSON deserialization) to a proper message object.
     * This handles remote messages that arrive as LinkedHashMap.
     */
    @SuppressWarnings("unchecked")
    private Object convertMapToMessage(Map<?, ?> map) {
        System.out.println("[DEBUG] convertMapToMessage - Keys: " + map.keySet());
        
        // Check if it contains a 'requester' field - likely RequestVilleInfo
        // This should be checked BEFORE unwrapping payload
        if (map.containsKey("requester")) {
            System.out.println("[DEBUG] Found 'requester' field - this is RequestVilleInfo");
            Object requesterObj = map.get("requester");
            if (requesterObj instanceof Map) {
                Map<String, Object> requesterMap = (Map<String, Object>) requesterObj;
                String actorId = (String) requesterMap.get("actorId");
                System.out.println("[DEBUG] Requester actorId: " + actorId);
                
                if (actorId != null && context != null) {
                    // Try to get the requester from local context first
                    ActorRef requester = context.actorFor(actorId);
                    
                    // If not found locally and we have remote transport capability
                    if (requester == null && context instanceof DefaultActorContext) {
                        DefaultActorContext defaultContext = (DefaultActorContext) context;
                        RemoteMessageTransport transport = defaultContext.getRemoteTransport();
                        
                        if (transport != null) {
                            System.out.println("[DEBUG] Creating RemoteActorRefProxy for: " + actorId);
                            // Create a RemoteActorRefProxy for the requester
                            requester = new RemoteActorRefProxy(actorId, transport, context.self());
                        }
                    }
                    
                    if (requester != null) {
                        System.out.println("[DEBUG] Created RequestVilleInfo with requester: " + requester.getActorId());
                        return new RequestVilleInfo(requester);
                    }
                }
            }
        }
        
        // Check if it contains a 'clientRef' field - likely RegisterClient
        // This should be checked BEFORE unwrapping payload (like ClientActor does with villeInfo)
        if (map.containsKey("clientRef")) {
            System.out.println("[DEBUG] Found 'clientRef' field - this is RegisterClient");
            Object clientRefObj = map.get("clientRef");
            if (clientRefObj instanceof Map) {
                Map<String, Object> clientRefMap = (Map<String, Object>) clientRefObj;
                String actorId = (String) clientRefMap.get("actorId");
                System.out.println("[DEBUG] ClientRef actorId: " + actorId);
                
                if (actorId != null && context != null) {
                    // Try to get the client from local context first
                    ActorRef clientRef = context.actorFor(actorId);
                    
                    // If not found locally and we have remote transport capability
                    if (clientRef == null && context instanceof DefaultActorContext) {
                        DefaultActorContext defaultContext = (DefaultActorContext) context;
                        RemoteMessageTransport transport = defaultContext.getRemoteTransport();
                        
                        if (transport != null) {
                            System.out.println("[DEBUG] Creating RemoteActorRefProxy for client: " + actorId);
                            // Create a RemoteActorRefProxy for the client
                            clientRef = new RemoteActorRefProxy(actorId, transport, context.self());
                        }
                    }
                    
                    if (clientRef != null) {
                        System.out.println("[DEBUG] Created RegisterClient with clientRef: " + clientRef.getActorId());
                        return new RegisterClient(clientRef);
                    }
                }
            }
        }
        
        // The message might be wrapped in a SimpleMessage structure
        // Check for nested payload structure - unwrap if we haven't found a specific message type
        if (map.containsKey("payload") && map.get("payload") instanceof Map) {
            System.out.println("[DEBUG] Found nested payload, unwrapping...");
            Map<String, Object> innerPayload = (Map<String, Object>) map.get("payload");
            // Recursively process the inner payload
            return convertMapToMessage(innerPayload);
        }
        
        System.out.println("[DEBUG] Could not convert map, returning as-is");
        // Return the map as-is if we can't convert it
        return map;
    }
    
    private void handleRegisterClient(RegisterClient req) {
        registeredClients.add(req.getClientRef());
        System.out.println("Client registered with " + name + ": " + req.getClientRef().getActorId());
    }
    
    private void handleUnregisterClient(UnregisterClient req) {
        registeredClients.remove(req.getClientRef());
        System.out.println("Client unregistered from " + name + ": " + req.getClientRef().getActorId());
    }
    
    private void handleCapteurDataUpdate(CapteurDataUpdate update) {
        latestReadings.put(update.getCapteurId(), update.getReading());
        System.out.println(name + " received sensor reading from " + update.getCapteurId());
    }
    
    /**
     * Handle request for ville information.
     * Responds with VilleInfoResponse containing city details.
     */
    private void handleRequestVilleInfo(RequestVilleInfo req) {
        System.out.println(name + " received info request from " + req.getRequester().getActorId());
        
        // Build VilleInfo response
        VilleInfo villeInfo = new VilleInfo(
            context != null ? context.self().getActorId() : "unknown",
            name,
            status,
            climateConfig,
            latestReadings.size() // Number of active sensors
        );
        
        // Send response back to requester
        VilleInfoResponse response = new VilleInfoResponse(villeInfo);
        req.getRequester().tell(new SimpleMessage(response), context != null ? context.self() : null);
        
        System.out.println(name + " sent info response to " + req.getRequester().getActorId());
    }
    
    private void handleStringCommand(String command) {
        if (command.equals("STATUS")) {
            System.out.println(name + " status: " + status + 
                             ", clients=" + registeredClients.size() + 
                             ", sensors=" + latestReadings.size());
        }
    }
    
    /**
     * Aggregate sensor data and broadcast to all registered clients.
     * Called every 5 seconds by scheduler.
     */
    private void broadcastClimateReport() {
        if (registeredClients.isEmpty() || !"ACTIVE".equals(status)) {
            return;
        }
        
        // Aggregate sensor data by type
        Map<String, Double> aggregated = new HashMap<>();
        Map<String, List<Double>> groupedByType = new HashMap<>();
        
        for (SensorReading reading : latestReadings.values()) {
            groupedByType
                .computeIfAbsent(reading.getSensorType(), k -> new ArrayList<>())
                .add(reading.getValue());
        }
        
        // Calculate averages
        for (Map.Entry<String, List<Double>> entry : groupedByType.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            aggregated.put(entry.getKey(), avg);
        }
        
        // Create climate report
        ClimateReport report = new ClimateReport(
            context != null ? context.self().getActorId() : "unknown",
            name,
            aggregated,
            latestReadings.size(),
            System.currentTimeMillis()
        );
        
        // Broadcast to all registered clients
        for (ActorRef client : registeredClients) {
            client.tell(new SimpleMessage(report), context != null ? context.self() : null);
        }
        
        System.out.println(name + " broadcasted climate report to " + registeredClients.size() + " clients");
    }
    
    @Override
    public void postStop() {
        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("VilleActor stopped: " + name);
    }
}
