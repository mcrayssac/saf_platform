package com.acme.iot.city.actors;

import com.acme.iot.city.messages.AssociateCapteurToVille;
import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.ClimateConfigUpdate;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.*;
import com.acme.saf.saf_runtime.messaging.*;

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
 * - Broadcast readings inter-pods via messaging broker for other instances
 */
public class CapteurActor implements Actor {
    
    // Configuration
    private final String sensorType;  // "TEMPERATURE", "PRESSURE", "HUMIDITY"
    private String villeId;           // Paris, etc - can be set later via AssociateCapteurToVille
    private String kafkaTopic;        // capteur-temperature-paris - updated when ville is set
    private final String location;    // Location within the city
    private ClimateConfig villeConfig;
    private ActorRef associatedVille;
    private String associatedVilleId; // Store ville ID for remote messaging
    private String status = "ACTIVE";
    
    // Scheduler for periodic sensor readings
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private ActorContext context;
    
    // Inter-pod messaging via Kafka
    private RemoteMessageTransport remoteTransport;
    
    public CapteurActor(Map<String, Object> params) {
        // Support both "type" and "sensorType" parameter names
        this.sensorType = (String) params.getOrDefault("sensorType", 
                                  params.getOrDefault("type", "TEMPERATURE"));
        this.villeId = (String) params.getOrDefault("villeId", "unknown");
        this.location = (String) params.getOrDefault("location", "unknown");
        this.kafkaTopic = "capteur-" + sensorType.toLowerCase() + "-" + villeId.toLowerCase();
        
        // Extract and parse climate config passed from the city actor
        this.villeConfig = parseClimateConfig(params.get("climateConfig"));
        System.err.println("✓ CapteurActor: Initialized " + sensorType + " at " + location + " (villeId=" + villeId + ")");
    }
    
    /**
     * Parse ClimateConfig from various input formats
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
            double variance = getDoubleValue(map, "temperatureVariance", 10.0);
            
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
    
    /**
     * Sets the associated ville actor reference.
     */
    public void setAssociatedVille(ActorRef villeRef) {
        this.associatedVille = villeRef;
    }
    
    @Override
    public void preStart() {
        System.out.println("CapteurActor started: type=" + sensorType);
        System.err.println("✓ CapteurActor preStart() called for type=" + sensorType);
        
        // Initialize remote transport for inter-pod messaging (Kafka)
        if (context != null && context instanceof DefaultActorContext) {
            DefaultActorContext defaultContext = (DefaultActorContext) context;
            this.remoteTransport = defaultContext.getRemoteTransport();
            if (remoteTransport != null) {
                System.err.println("✓ CapteurActor: Remote transport initialized for Kafka messaging");
            } else {
                System.err.println("⚠ CapteurActor: No remote transport available");
            }
        }
        
        // Start periodic sensor readings (every 5 seconds, start after 2s)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::generateAndSendReading, 2, 5, TimeUnit.SECONDS);
        System.err.println("✓ CapteurActor: Scheduler started");
    }
    
    @Override
    public void receive(Message message) throws Exception {
        Object payload = message.getPayload();
        
        // Unwrap SimpleMessage if needed
        if (payload instanceof SimpleMessage simpleMsg) {
            payload = simpleMsg.getPayload();
        }
        
        // Handle Map (from JSON deserialization)
        if (payload instanceof Map) {
            payload = convertMapToMessage((Map<?, ?>) payload);
        }
        
        if (payload instanceof AssociateCapteurToVille assoc) {
            handleAssociateCapteurToVille(assoc);
        }
        else if (payload instanceof ClimateConfigUpdate update) {
            handleClimateConfigUpdate(update);
        }
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("CapteurActor received unknown message: " + payload.getClass().getName());
        }
    }
    
    /**
     * Convert a Map (from JSON deserialization) to a proper message object.
     */
    @SuppressWarnings("unchecked")
    private Object convertMapToMessage(Map<?, ?> map) {
        // Check for AssociateCapteurToVille
        if (map.containsKey("villeId") && map.containsKey("villeName")) {
            String villeId = (String) map.get("villeId");
            String villeName = (String) map.get("villeName");
            Object configObj = map.get("climateConfig");
            ClimateConfig config = configObj != null ? parseClimateConfig(configObj) : null;
            return new AssociateCapteurToVille(villeId, villeName, config);
        }
        
        // Check for ClimateConfigUpdate
        if (map.containsKey("config")) {
            Object configObj = map.get("config");
            ClimateConfig config = parseClimateConfig(configObj);
            return new ClimateConfigUpdate(config);
        }
        
        return map;
    }
    
    /**
     * Handle association of this capteur to a ville.
     * Called when ville receives RegisterCapteur and sends back this message.
     */
    private void handleAssociateCapteurToVille(AssociateCapteurToVille assoc) {
        this.associatedVilleId = assoc.getVilleId();
        this.villeId = extractCityName(assoc.getVilleId(), assoc.getVilleName());
        this.kafkaTopic = "capteur-" + sensorType.toLowerCase() + "-" + villeId.toLowerCase();
        
        if (assoc.getClimateConfig() != null) {
            this.villeConfig = assoc.getClimateConfig();
        }
        
        // Create a remote reference to the ville using Kafka transport
        if (context != null && context instanceof DefaultActorContext) {
            DefaultActorContext defaultContext = (DefaultActorContext) context;
            RemoteMessageTransport transport = defaultContext.getRemoteTransport();
            
            if (transport != null) {
                this.associatedVille = new RemoteActorRefProxy(assoc.getVilleId(), transport, context.self());
                System.out.println("✓ CapteurActor " + sensorType + " associated with ville " + assoc.getVilleId() + " via Kafka");
            }
        }
        
        System.out.println("✓ CapteurActor " + sensorType + " now associated with ville: " + villeId);
    }
    
    /**
     * Extract city name from villeId or villeName.
     */
    private String extractCityName(String villeId, String villeName) {
        if (villeName != null && !villeName.isEmpty()) {
            return villeName.toLowerCase();
        }
        // villeId might be a UUID, in which case we keep "unknown"
        if (villeId != null && !villeId.contains("-")) {
            return villeId.toLowerCase();
        }
        return "unknown";
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
     * Generate a random sensor reading and send to associated ville via Kafka.
     * Called every 5 seconds by scheduler.
     * Sends ONLY to the associated ville actor (not broadcast).
     */
    private void generateAndSendReading() {
        try {
            // Skip if not in active status
            if (!"ACTIVE".equals(status)) {
                return;
            }
            
            // Skip if no associated ville
            if (associatedVilleId == null || associatedVilleId.isEmpty()) {
                return;
            }
            
            // Generate base value (use defaults if no config)
            double baseValue = getBaseValueFromConfig();
            
            // Calculate variance range
            double variancePercent = (villeConfig != null ? villeConfig.getVariancePercentage() : 10.0) / 100.0;
            double variance = baseValue * variancePercent;
            
            // Generate random value: base ± variance
            double randomValue = baseValue + (random.nextDouble() * 2 - 1) * variance;
            
            // Create sensor reading
            SensorReading reading = new SensorReading(
                sensorType,
                randomValue,
                System.currentTimeMillis()
            );
            
            // Create update message with capteur ID
            String capteurId = context != null ? context.self().getActorId() : "unknown";
            CapteurDataUpdate update = new CapteurDataUpdate(capteurId, reading);
            
            // Send to associated ville via Kafka using RemoteActorRefProxy
            if (associatedVille != null) {
                try {
                    associatedVille.tell(new SimpleMessage(update), context != null ? context.self() : null);
                    System.out.println("✓ Capteur " + sensorType + " sent " + reading.getSensorType() + 
                                       " to ville " + associatedVilleId + ": " + String.format("%.2f", randomValue));
                } catch (Exception e) {
                    System.err.println("✗ Failed to send reading to ville " + associatedVilleId + ": " + e.getMessage());
                }
            } else {
                System.err.println("⚠ Capteur " + sensorType + " has no associatedVille reference (villeId=" + associatedVilleId + ")");
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in generateAndSendReading: " + e.getMessage());
            e.printStackTrace();
        }
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
