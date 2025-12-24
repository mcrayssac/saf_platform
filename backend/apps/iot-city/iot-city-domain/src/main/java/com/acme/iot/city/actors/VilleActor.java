package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.ClimateReport;
import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.*;
import com.acme.saf.saf_runtime.messaging.*;

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
 * - Receive sensor readings from other pods via inter-pod messaging
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
    
    // Inter-pod messaging
    private InterPodMessaging interPodMessaging;
    private MessageConsumer consumer;
    private static final String SENSOR_READINGS_TOPIC = "iot-city-sensor-readings";
    
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
        
        // Initialize inter-pod messaging system (only once, reuse if already initialized)
        try {
            // Try to get existing instance first
            try {
                this.interPodMessaging = InterPodMessaging.getInstance();
                System.out.println("VilleActor: Reusing existing InterPodMessaging instance");
            } catch (IllegalStateException e) {
                // Not initialized yet, initialize it
                System.out.println("VilleActor: Initializing new InterPodMessaging instance");
                MessagingConfiguration config = new MessagingConfiguration();
                this.interPodMessaging = config.initializeMessaging();
            }
            
            this.consumer = interPodMessaging.getConsumer();
            
            // Subscribe to sensor readings from other pods
            consumer.subscribe(
                CapteurDataUpdate.class.getName(),
                CapteurDataUpdate.class,
                update -> {
                    System.out.println(name + " received inter-pod sensor reading from " + update.getCapteurId());
                    latestReadings.put(update.getCapteurId(), update.getReading());
                }
            );
            
            // Start listening to sensor readings topic
            consumer.listen(SENSOR_READINGS_TOPIC);
            System.out.println("VilleActor: Inter-pod messaging initialized and listening on " + SENSOR_READINGS_TOPIC);
        } catch (Exception e) {
            System.err.println("VilleActor: Failed to initialize messaging - " + e.getMessage());
            this.interPodMessaging = null;
        }
        
        // Start periodic climate report broadcasting (every 5 seconds)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::broadcastClimateReport, 5, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public void receive(Message message) throws Exception {
        Object payload = message.getPayload();
        
        if (payload instanceof RegisterClient req) {
            handleRegisterClient(req);
        }
        else if (payload instanceof UnregisterClient req) {
            handleUnregisterClient(req);
        }
        else if (payload instanceof CapteurDataUpdate update) {
            handleCapteurDataUpdate(update);
        }
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("VilleActor received unknown message: " + payload.getClass().getName());
        }
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
        
        // Shutdown inter-pod messaging
        if (interPodMessaging != null) {
            try {
                interPodMessaging.shutdown();
                System.out.println("VilleActor: Inter-pod messaging shutdown");
            } catch (Exception e) {
                System.err.println("VilleActor: Error shutting down messaging - " + e.getMessage());
            }
        }
        
        System.out.println("VilleActor stopped: " + name);
    }
}
