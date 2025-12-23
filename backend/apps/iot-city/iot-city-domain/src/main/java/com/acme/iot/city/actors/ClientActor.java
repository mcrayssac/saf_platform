package com.acme.iot.city.actors;

import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.UnregisterClient;
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
        
        // Handle ClimateReport - received from VilleActor
        if (payload instanceof ClimateReport report) {
            handleClimateReport(report);
        }
        // Handle String commands for simple operations
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("ClientActor received unknown message type: " + 
                             payload.getClass().getName());
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
     * Handle simple string commands (for testing/demo purposes).
     * Format: "ENTER:villeId" or "LEAVE"
     */
    private void handleStringCommand(String command) {
        if (command.startsWith("ENTER:")) {
            String villeId = command.substring(6);
            handleEnterVille(villeId);
        } else if (command.equals("LEAVE")) {
            handleLeaveVille();
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
        
        // Find the ville actor using framework's actor lookup
        currentVilleRef = context.actorFor(villeId);
        
        if (currentVilleRef != null) {
            currentVilleId = villeId;
            
            // Register with ville to receive climate reports
            currentVilleRef.tell(new SimpleMessage(new RegisterClient(context.self())), context.self());
            
            System.out.println("ClientActor entered ville: " + villeId);
        } else {
            System.err.println("Ville not found: " + villeId);
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
    
    @Override
    public void postStop() {
        // Clean up - leave ville if currently in one
        if (currentVilleId != null) {
            handleLeaveVille();
        }
        System.out.println("ClientActor stopped: sessionId=" + sessionId);
    }
}
