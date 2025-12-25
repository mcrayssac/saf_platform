package com.acme.iot.city.actors;

import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.RequestVilleInfo;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.city.messages.VilleInfoResponse;
import com.acme.iot.city.model.ClimateReport;
import com.acme.saf.actor.core.*;

import java.util.Map;

/**
 * Represents a client (user) in the IoT City system.
 * Can receive climate reports from cities via WebSocket.
 * 
 * Responsibilities:
 * - Enter/leave cities (register/unregister for climate updates)
 * - Receive climate reports and forward to WebSocket
 * - Handle basic client operations
 */
public class ClientActor implements Actor {
    
    // State
    private final String sessionId;
    private String currentVilleId;
    private ActorRef currentVilleRef;
    
    // Context (will be injected by the framework)
    private ActorContext context;
    
    public ClientActor(Map<String, Object> params) {
        this.sessionId = (String) params.getOrDefault("sessionId", "unknown");
    }
    
    /**
     * Sets the actor context. This should be called by the framework after actor creation.
     */
    public void setContext(ActorContext context) {
        this.context = context;
    }
    
    @Override
    public void preStart() {
        System.out.println("ClientActor started: sessionId=" + sessionId);
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
        
        // Handle ClimateReport - received from VilleActor
        if (payload instanceof ClimateReport report) {
            handleClimateReport(report);
        }
        // Handle VilleInfoResponse - received from VilleActor
        else if (payload instanceof VilleInfoResponse response) {
            handleVilleInfoResponse(response);
        }
        // Handle String commands for simple operations
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        // Handle Map commands from frontend (format: { command: "ENTER:villeId" })
        else if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            Object commandObj = map.get("command");
            if (commandObj instanceof String) {
                handleStringCommand((String) commandObj);
            } else {
                System.out.println("ClientActor received Map without command: " + map);
            }
        }
        else {
            System.out.println("ClientActor received unknown message type: " + 
                             payload.getClass().getName());
        }
    }
    
    /**
     * Convert a Map (from JSON deserialization) to a proper message object.
     * This handles remote messages that arrive as LinkedHashMap.
     */
    @SuppressWarnings("unchecked")
    private Object convertMapToMessage(Map<?, ?> map) {
        // Check if it contains a 'villeInfo' field - likely VilleInfoResponse
        if (map.containsKey("villeInfo")) {
            Object villeInfoObj = map.get("villeInfo");
            if (villeInfoObj instanceof Map) {
                Map<String, Object> villeInfoMap = (Map<String, Object>) villeInfoObj;
                return new VilleInfoResponse(convertToVilleInfo(villeInfoMap));
            }
        }
        
        // The message might be wrapped in a SimpleMessage structure
        // Check for nested payload structure
        if (map.containsKey("payload") && map.get("payload") instanceof Map) {
            Map<String, Object> innerPayload = (Map<String, Object>) map.get("payload");
            // Recursively process the inner payload
            return convertMapToMessage(innerPayload);
        }
        
        // Return the map as-is if we can't convert it
        return map;
    }
    
    /**
     * Convert a Map to VilleInfo object.
     */
    @SuppressWarnings("unchecked")
    private com.acme.iot.city.model.VilleInfo convertToVilleInfo(Map<String, Object> map) {
        String villeId = (String) map.get("villeId");
        String name = (String) map.get("name");
        String status = (String) map.get("status");
        Integer capteursCount = (Integer) map.get("capteursCount");
        
        // Parse climate config
        com.acme.iot.city.model.ClimateConfig climateConfig = null;
        Object climateConfigObj = map.get("climateConfig");
        if (climateConfigObj instanceof Map) {
            Map<String, Object> ccMap = (Map<String, Object>) climateConfigObj;
            double meanTemp = getDoubleValue(ccMap, "meanTemperature", 20.0);
            double meanPressure = getDoubleValue(ccMap, "meanPressure", 1013.0);
            double meanHumidity = getDoubleValue(ccMap, "meanHumidity", 60.0);
            double variance = getDoubleValue(ccMap, "variancePercentage", 10.0);
            climateConfig = new com.acme.iot.city.model.ClimateConfig(meanTemp, meanPressure, meanHumidity, variance);
        }
        
        return new com.acme.iot.city.model.VilleInfo(villeId, name, status, climateConfig, capteursCount != null ? capteursCount : 0);
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
     * Handle climate report by forwarding to WebSocket client.
     */
    private void handleClimateReport(ClimateReport report) {
        System.out.println("ClientActor received climate report from " + report.getVilleName());
        
        // Forward to WebSocket if context is available
        if (context != null && context.hasWebSocketConnection()) {
            context.sendToWebSocket(report);
            System.out.println("Climate report sent to WebSocket for client: " + sessionId);
        } else {
            System.out.println("No WebSocket connection for client: " + sessionId);
        }
    }
    
    /**
     * Handle VilleInfoResponse by forwarding to WebSocket client.
     */
    private void handleVilleInfoResponse(VilleInfoResponse response) {
        System.out.println("ClientActor received ville info for " + response.getVilleInfo().getName());
        
        // Forward to WebSocket if context is available
        if (context != null && context.hasWebSocketConnection()) {
            context.sendToWebSocket(response.getVilleInfo());
            System.out.println("Ville info sent to WebSocket for client: " + sessionId);
        } else {
            System.out.println("No WebSocket connection for client: " + sessionId);
        }
    }
    
    /**
     * Handle simple string commands (for testing/demo purposes).
     * Format: "ENTER:villeId", "LEAVE", or "GET_VILLE_INFO:villeId"
     * Can also receive Map with "command" key from frontend
     */
    private void handleStringCommand(String command) {
        if (command.startsWith("ENTER:")) {
            String villeId = command.substring(6);
            handleEnterVille(villeId);
        } else if (command.equals("LEAVE")) {
            handleLeaveVille();
        } else if (command.startsWith("GET_VILLE_INFO:")) {
            String villeId = command.substring(15);
            handleGetVilleInfo(villeId);
        } else {
            System.out.println("ClientActor received command: " + command);
        }
    }
    
    /**
     * Enter a city to start receiving climate updates.
     */
    private void handleEnterVille(String villeId) {
        // Leave current ville if any
        if (currentVilleId != null) {
            handleLeaveVille();
        }
        
        if (context == null) {
            System.err.println("Cannot enter ville: context not available");
            return;
        }
        
        // Try local lookup first
        currentVilleRef = context.actorFor(villeId);
        
        if (currentVilleRef != null) {
            currentVilleId = villeId;
            
            // Register with ville to receive climate reports
            currentVilleRef.tell(new SimpleMessage(new RegisterClient(context.self())), context.self());
            
            System.out.println("ClientActor entered ville: " + villeId);
        } else {
            // Actor not found locally - try remote via transport
            if (context instanceof DefaultActorContext defaultContext && 
                defaultContext.getRemoteTransport() != null) {
                
                currentVilleId = villeId;
                currentVilleRef = new RemoteActorRefProxy(villeId, defaultContext.getRemoteTransport(), context.self());
                
                // Register with remote ville
                currentVilleRef.tell(new SimpleMessage(new RegisterClient(context.self())), context.self());
                
                System.out.println("ClientActor entered remote ville: " + villeId);
            } else {
                System.err.println("Ville not found: " + villeId);
            }
        }
    }
    
    /**
     * Leave the current city.
     */
    private void handleLeaveVille() {
        if (currentVilleId != null && currentVilleRef != null && context != null) {
            // Unregister from ville
            currentVilleRef.tell(new SimpleMessage(new UnregisterClient(context.self())), context.self());
            
            System.out.println("ClientActor left ville: " + currentVilleId);
            
            currentVilleId = null;
            currentVilleRef = null;
        }
    }
    
    /**
     * Request ville information from a specific ville.
     * This is called by the frontend to get detailed city information.
     */
    private void handleGetVilleInfo(String villeId) {
        if (context == null) {
            System.err.println("Cannot get ville info: context not available");
            return;
        }
        
        // Try local lookup first
        ActorRef villeRef = context.actorFor(villeId);
        
        if (villeRef != null) {
            // Request info from local ville
            villeRef.tell(new SimpleMessage(new RequestVilleInfo(context.self())), context.self());
            
            System.out.println("ClientActor requested info from ville: " + villeId);
        } else {
            // Actor not found locally - try remote via transport
            if (context instanceof DefaultActorContext defaultContext && 
                defaultContext.getRemoteTransport() != null) {
                
                villeRef = new RemoteActorRefProxy(villeId, defaultContext.getRemoteTransport(), context.self());
                
                // Request info from remote ville
                villeRef.tell(new SimpleMessage(new RequestVilleInfo(context.self())), context.self());
                
                System.out.println("ClientActor requested info from remote ville: " + villeId);
            } else {
                System.err.println("Ville not found: " + villeId);
            }
        }
    }
    
    @Override
    public void postStop() {
        // Clean up - leave ville if currently in one
        if (currentVilleId != null) {
            handleLeaveVille();
        }
        System.out.println("ClientActor stopped: sessionId=" + sessionId);
    }
}
