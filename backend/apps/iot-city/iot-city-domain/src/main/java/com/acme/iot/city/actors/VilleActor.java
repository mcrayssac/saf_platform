package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.city.messages.WeatherRequest;
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
    private final String actorId;     // paris, lyon, etc
    private final String name;
    private final ClimateConfig climateConfig;
    private String status = "ACTIVE";
    
    // Runtime state
    private final Map<String, ActorRef> registeredClients = new ConcurrentHashMap<>();
    private final Map<String, SensorReading> latestReadings = new ConcurrentHashMap<>();
    private final Map<String, ActorRef> sensors = new ConcurrentHashMap<>();  // Store sensor references
    
    // Scheduler for periodic climate reports
    private ScheduledExecutorService scheduler;
    private ActorContext context;
    
    // Inter-pod messaging
    private InterPodMessaging interPodMessaging;
    private MessageConsumer consumer;  // For publishing weather reports
    private Map<String, MessageConsumer> sensorConsumers;  // Separate consumer for each sensor topic
    private String weatherTopicName;  // Topic where climate reports are published
    private static final String SENSOR_READINGS_TOPIC = "iot-city-sensor-readings";
    
    public VilleActor(Map<String, Object> params) {
        // Get actorId
        String actorIdParam = (String) params.getOrDefault("actorId", "UnknownCity");
        this.actorId = actorIdParam;
        
        // Try to get name from params first (villeName or nom)
        String nameFromParams = (String) params.getOrDefault("villeName", 
                                    params.getOrDefault("nom", null));
        
        // If no name in params, use actorId and capitalize it (e.g., "paris" -> "Paris")
        if (nameFromParams == null) {
            this.name = actorIdParam.substring(0, 1).toUpperCase() + actorIdParam.substring(1).toLowerCase();
        } else {
            this.name = nameFromParams;
        }
        
        // Create weather topic name for this city
        this.weatherTopicName = "ville-" + actorId + "-weather";
        
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
            
            // Create 3 sensors for this city and subscribe to their topics
            createAndSubscribeSensors();
            
            System.out.println("VilleActor: Inter-pod messaging initialized");
        } catch (Exception e) {
            System.err.println("VilleActor: Failed to initialize messaging - " + e.getMessage());
            this.interPodMessaging = null;
        }
        
        // Start periodic climate report broadcasting (every 5 seconds)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::broadcastClimateReport, 5, 5, TimeUnit.SECONDS);
        
        // Subscribe to 3 sensor topics
        createAndSubscribeSensors();
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
        else if (payload instanceof WeatherRequest req) {
            handleWeatherRequest(req);
        }
        else if (payload instanceof String command) {
            handleStringCommand(command);
        }
        else {
            System.out.println("VilleActor received unknown message: " + payload.getClass().getName());
        }
    }
    
    private void handleRegisterClient(RegisterClient req) {
        // Handle both ActorRef-based (real actors) and clientId-based (REST API testing)
        if (req.getClientRef() != null) {
            registeredClients.put(req.getClientRef().getActorId(), req.getClientRef());
            System.out.println("Client registered with " + name + ": " + req.getClientRef().getActorId());
        } else if (req.getClientId() != null) {
            // For REST API testing - store with a dummy ActorRef (null ActorRef not allowed in ConcurrentHashMap)
            registeredClients.put(req.getClientId(), new DummyActorRef(req.getClientId()));
            System.out.println("Client registered with " + name + ": " + req.getClientId());
        }
    }
    
    private void handleUnregisterClient(UnregisterClient req) {
        // Handle both ActorRef-based and clientId-based messaging
        if (req.getClientRef() != null) {
            registeredClients.remove(req.getClientRef().getActorId());
            System.out.println("Client unregistered from " + name + ": " + req.getClientRef().getActorId());
        } else if (req.getClientId() != null) {
            registeredClients.remove(req.getClientId());
            System.out.println("Client unregistered from " + name + ": " + req.getClientId());
        }
    }
    
    private void handleCapteurDataUpdate(CapteurDataUpdate update) {
        latestReadings.put(update.getCapteurId(), update.getReading());
        System.out.println(name + " received sensor reading from " + update.getCapteurId());
    }
    
    private void handleWeatherRequest(WeatherRequest req) {
        System.out.println(name + " received weather request from " + req.getClientId());
        
        // Aggregate current sensor data
        Map<String, Double> aggregated = aggregateSensorData();
        
        // Create and send climate report back
        ClimateReport report = new ClimateReport(
            context != null ? context.self().getActorId() : "unknown",
            name,
            aggregated,
            latestReadings.size(),
            System.currentTimeMillis()
        );
        
        System.out.println(name + " sending weather response with " + latestReadings.size() + " sensors");
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
        if (!"ACTIVE".equals(status)) {
            return;
        }
        
        Map<String, Double> aggregated = aggregateSensorData();
        
        // Create climate report
        ClimateReport report = new ClimateReport(
            context != null ? context.self().getActorId() : "unknown",
            name,
            aggregated,
            latestReadings.size(),
            System.currentTimeMillis()
        );
        
        // Publish to Kafka topic for this city's weather
        try {
            if (interPodMessaging != null) {
                interPodMessaging.getProducer().send(report, weatherTopicName);
                System.out.println(name + " published climate report to Kafka topic: " + weatherTopicName);
            } else {
                System.out.println(name + " - Warning: InterPodMessaging not initialized, skipping Kafka publish");
            }
        } catch (Exception e) {
            System.err.println(name + " - Error publishing climate report to Kafka: " + e.getMessage());
        }
    }
    
    private void createAndSubscribeSensors() {
        try {
            sensorConsumers = new ConcurrentHashMap<>();
            String[] sensorTypes = {"TEMPERATURE", "HUMIDITY", "PRESSURE"};
            
            // Create a serializer for consumers
            MessageSerializer serializer = new JacksonMessageSerializer();
            
            for (String sensorType : sensorTypes) {
                String sensorId = sensorType.toLowerCase() + "-" + actorId;
                String kafkaTopic = "capteur-" + sensorType.toLowerCase() + "-" + actorId;
                
                // Create a separate consumer for each sensor topic
                MessageConsumer sensorConsumer = new DefaultMessageConsumer(serializer);
                
                // Subscribe to sensor readings
                sensorConsumer.subscribe(
                    SensorReading.class.getName(),
                    SensorReading.class,
                    reading -> {
                        System.out.println(name + " received " + reading.getSensorType() + " reading: " + reading.getValue());
                        latestReadings.put(sensorId, reading);
                    }
                );
                
                // Start listening to this specific topic
                sensorConsumer.listen(kafkaTopic);
                sensorConsumers.put(sensorType, sensorConsumer);
                System.out.println("VilleActor: Listening to sensor topic " + kafkaTopic);
            }
        } catch (Exception e) {
            System.err.println("VilleActor: Error subscribing to sensor topics - " + e.getMessage());
            e.printStackTrace();
        }
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
        
        // Shutdown sensor consumers
        if (sensorConsumers != null) {
            for (MessageConsumer sensorConsumer : sensorConsumers.values()) {
                try {
                    sensorConsumer.stopListening(null); // Stop all listening
                } catch (Exception e) {
                    // Ignore, it's shutdown time anyway
                }
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
    
    /**
     * Aggregate sensor readings by type and calculate averages.
     */
    public Map<String, Double> aggregateSensorData() {
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
        
        return aggregated;
    }
    
    /**
     * Get the latest readings map (for direct access by REST API).
     */
    public Map<String, SensorReading> getLatestReadings() {
        return latestReadings;
    }
    
    /**
     * Get the city name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the number of registered clients.
     */
    public int getRegisteredClientsCount() {
        return registeredClients.size();
    }

    /**
     * Get the climate configuration for this city.
     */
    public ClimateConfig getClimateConfig() {
        return climateConfig;
    }

    /**
     * DummyActorRef for REST API testing - represents a client without a real ActorRef
     */
    public static class DummyActorRef implements ActorRef {
        private final String actorId;

        public DummyActorRef(String actorId) {
            this.actorId = actorId;
        }

        @Override
        public String getActorId() {
            return actorId;
        }

        @Override
        public String getPath() {
            return "/dummy/" + actorId;
        }

        @Override
        public void tell(Message message) {
            // No-op for REST API test clients
        }

        @Override
        public void tell(Message message, ActorRef sender) {
            // No-op for REST API test clients
        }

        @Override
        public java.util.concurrent.CompletableFuture<Object> ask(Message message, long timeout, java.util.concurrent.TimeUnit unit) {
            // Return a completed future for dummy clients
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("DummyActorRef doesn't support ask pattern"));
        }

        @Override
        public void forward(Message message, ActorRef originalSender) {
            // No-op for REST API test clients
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void stop() {
            // No-op for REST API test clients
        }

        @Override
        public void block() {
            // No-op for REST API test clients
        }

        @Override
        public void unblock() {
            // No-op for REST API test clients
        }

        @Override
        public void restart(Throwable cause) {
            // No-op for REST API test clients
        }

        @Override
        public com.acme.saf.actor.core.ActorLifecycleState getState() {
            return com.acme.saf.actor.core.ActorLifecycleState.RUNNING;
        }

        @Override
        public void watch(ActorRef watcher) {
            // No-op for REST API test clients
        }

        @Override
        public void unwatch(ActorRef watcher) {
            // No-op for REST API test clients
        }

        @Override
        public String toString() {
            return "DummyActorRef(" + actorId + ")";
        }
    }
}

