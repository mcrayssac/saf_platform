package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.ClimateConfigUpdate;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.*;
import com.acme.saf.saf_runtime.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(CapteurActor.class);
    
    // Configuration
    private final String sensorType;  // "TEMPERATURE", "PRESSURE", "HUMIDITY"
    private final String villeId;     // Paris, etc
    private final String kafkaTopic;  // capteur-temperature-paris
    private ClimateConfig villeConfig;
    private ActorRef associatedVille;
    private String status = "ACTIVE";
    
    // Scheduler for periodic sensor readings
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private ActorContext context;
    
    // Inter-pod messaging
    private InterPodMessaging interPodMessaging;
    private MessageProducer producer;
    private static final String SENSOR_READINGS_TOPIC = "iot-city-sensor-readings";
    
    public CapteurActor(Map<String, Object> params) {
        this.sensorType = (String) params.getOrDefault("type", "TEMPERATURE");
        this.villeId = (String) params.getOrDefault("villeId", "unknown");
        this.kafkaTopic = "capteur-" + sensorType.toLowerCase() + "-" + villeId;
        
        // Extract and parse climate config passed from the city actor
        this.villeConfig = parseClimateConfig(params.get("climateConfig"));
        System.err.println("✓ CapteurActor: Initialized with climateConfig for " + sensorType + "-" + villeId + ": " + villeConfig);
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
        
        // Initialize inter-pod messaging system (only once, reuse if already initialized)
        try {
            System.err.println("→ Getting or initializing MessagingConfiguration...");
            
            // Try to get existing instance first
            try {
                this.interPodMessaging = InterPodMessaging.getInstance();
                System.err.println("✓ Reusing existing InterPodMessaging instance");
            } catch (IllegalStateException e) {
                // Not initialized yet, initialize it
                System.err.println("→ No existing instance, initializing MessagingConfiguration...");
                MessagingConfiguration config = new MessagingConfiguration();
                this.interPodMessaging = config.initializeMessaging();
                System.err.println("✓ New InterPodMessaging instance created");
            }
            
            this.producer = interPodMessaging.getProducer();
            System.err.println("✓ CapteurActor: Inter-pod messaging initialized, producer=" + producer);
        } catch (Exception e) {
            System.err.println("✗ CapteurActor: Failed to initialize messaging - " + e.getMessage());
            e.printStackTrace(System.err);
            this.interPodMessaging = null;
        }
        
        // Start periodic sensor readings (every 5 seconds, start after 2s)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::generateAndSendReading, 2, 5, TimeUnit.SECONDS);
        System.err.println("✓ CapteurActor: Scheduler started");
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
     * Also broadcasts to other pods via messaging broker.
     */
    private void generateAndSendReading() {
        try {
            logger.info("[SCHEDULER] generateAndSendReading called for {}", sensorType);
            
            // Skip if not in active status
            if (!"ACTIVE".equals(status)) {
                logger.warn("Skipping - not ACTIVE");
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
            
            // Send to associated ville locally (if it exists)
            String capteurId = context != null ? context.self().getActorId() : "unknown";
            CapteurDataUpdate update = new CapteurDataUpdate(capteurId, reading);
            
            if (associatedVille != null) {
                associatedVille.tell(new SimpleMessage(update), context != null ? context.self() : null);
            }
            
            // Broadcast to other pods via messaging broker on individual topic
            logger.info("Checking producer: producer={}, isConnected={}", producer, producer != null ? producer.isConnected() : "null");
            if (producer != null && producer.isConnected()) {
                try {
                    logger.info("Sending to Kafka topic: {} ...", kafkaTopic);
                    // Send SensorReading directly to individual capteur topic
                    producer.sendAsync(reading, kafkaTopic);
                    logger.info("✓ Capteur {} broadcast to {}: {}", sensorType, kafkaTopic, String.format("%.2f", randomValue));
                } catch (Exception e) {
                    logger.error("✗ Failed to broadcast reading to {}: {}", kafkaTopic, e.getMessage(), e);
                }
            } else {
                logger.warn("✗ Capteur {} producer not connected or null", sensorType);
            }
        } catch (Exception e) {
            logger.error("Unexpected error in generateAndSendReading", e);
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
        
        // Shutdown inter-pod messaging
        if (interPodMessaging != null) {
            try {
                interPodMessaging.shutdown();
                System.out.println("CapteurActor: Inter-pod messaging shutdown");
            } catch (Exception e) {
                System.err.println("CapteurActor: Error shutting down messaging - " + e.getMessage());
            }
        }
        
        System.out.println("CapteurActor stopped: " + sensorType);
    }
}
