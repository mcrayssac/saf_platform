package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.ClimateConfigUpdate;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Represents an IoT sensor (capteur) in the system.
 * 
 * Responsibilities:
 * - Generate sensor readings based on city's climate config
 * - Send readings to associated city every 5 seconds
 * - Readings have random variance around configured mean values
 */
public class CapteurActor implements Actor {
    
    // Configuration
    private final String sensorType;  // "TEMPERATURE", "PRESSURE", "HUMIDITY"
    private ClimateConfig villeConfig;
    private ActorRef associatedVille;
    private String status = "ACTIVE";
    
    // Scheduler for periodic sensor readings
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private ActorContext context;
    
    public CapteurActor(Map<String, Object> params) {
        this.sensorType = (String) params.getOrDefault("type", "TEMPERATURE");
    }
    
    /**
     * Sets the actor context. Called by framework after actor creation.
     */
    public void setContext(ActorContext context) {
        this.context = context;
    }
    
    /**
     * Sets the associated ville actor reference.
     */
    public void setAssociatedVille(ActorRef villeRef) {
        this.associatedVille = villeRef;
    }
    
    @Override
    public void preStart() {
        System.out.println("CapteurActor started: type=" + sensorType);
        
        // Start periodic sensor readings (every 5 seconds, start after 2s)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::generateAndSendReading, 2, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public void receive(Message message) throws Exception {
        Object payload = message.getPayload();
        
        if (payload instanceof ClimateConfigUpdate update) {
            handleClimateConfigUpdate(update);
        }
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("CapteurActor received unknown message: " + payload.getClass().getName());
        }
    }
    
    private void handleClimateConfigUpdate(ClimateConfigUpdate update) {
        this.villeConfig = update.getConfig();
        System.out.println("CapteurActor received climate config");
    }
    
    private void handleStringCommand(String command) {
        if (command.equals("STATUS")) {
            System.out.println("Capteur " + sensorType + " status: " + status);
        }
        else if (command.startsWith("ASSOCIATE:")) {
            // Extract ville actor ID and look it up
            String villeId = command.substring(10);
            if (context != null) {
                this.associatedVille = context.actorFor(villeId);
                System.out.println("Capteur " + sensorType + " associated with ville: " + villeId);
            }
        }
    }
    
    /**
     * Generate a random sensor reading and send to associated ville.
     * Called every 5 seconds by scheduler.
     */
    private void generateAndSendReading() {
        if (associatedVille == null || villeConfig == null || !"ACTIVE".equals(status)) {
            return;
        }
        
        // Get base value from climate config
        double baseValue = getBaseValueFromConfig();
        
        // Calculate variance range
        double variancePercent = villeConfig.getVariancePercentage() / 100.0;
        double variance = baseValue * variancePercent;
        
        // Generate random value: base ± variance
        double randomValue = baseValue + (random.nextDouble() * 2 - 1) * variance;
        
        // Create sensor reading
        SensorReading reading = new SensorReading(
            sensorType,
            randomValue,
            System.currentTimeMillis()
        );
        
        // Send to associated ville
        String capteurId = context != null ? context.self().getActorId() : "unknown";
        CapteurDataUpdate update = new CapteurDataUpdate(capteurId, reading);
        
        associatedVille.tell(new SimpleMessage(update), context != null ? context.self() : null);
        
        System.out.println("Capteur " + sensorType + " sent reading: " + String.format("%.2f", randomValue));
    }
    
    /**
     * Get base value from ville's climate configuration based on sensor type.
     */
    private double getBaseValueFromConfig() {
        if (villeConfig == null) {
            return 0.0;
        }
        
        return switch (sensorType) {
            case "TEMPERATURE" -> villeConfig.getMeanTemperature();
            case "PRESSURE" -> villeConfig.getMeanPressure();
            case "HUMIDITY" -> villeConfig.getMeanHumidity();
            default -> 0.0;
        };
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
        System.out.println("CapteurActor stopped: " + sensorType);
    }
}
