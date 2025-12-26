package com.acme.iot.city.actors;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.messages.RegisterCapteur;
import com.acme.iot.city.messages.RegisterClient;
import com.acme.iot.city.messages.RequestVilleInfo;
import com.acme.iot.city.messages.UnregisterClient;
import com.acme.iot.city.messages.VilleInfoResponse;
import com.acme.iot.city.messages.WeatherRequest;
import com.acme.iot.city.model.ClimateConfig;
import com.acme.iot.city.model.ClimateReport;
import com.acme.iot.city.model.SensorReading;
import com.acme.iot.city.model.CapteurInfo;
import com.acme.saf.actor.core.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a city in the IoT monitoring system.
 * 
 * Responsibilities:
 * - Maintain climate configuration
 * - Track registered clients (who entered this city)
 * - Track registered sensors (capteurs)
 * - Collect sensor readings from associated capteurs
 * - Aggregate data and broadcast climate reports
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
    private final Map<String, CapteurInfo> registeredCapteurs = new ConcurrentHashMap<>();
    
    // Scheduler for periodic climate reports
    private ScheduledExecutorService scheduler;
    protected ActorContext context;
    
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
        System.out.println("VilleActor started: " + name + " (actorId=" + actorId + ")");
        
        // Start periodic climate report broadcasting (every 10 seconds)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::broadcastClimateReport, 10, 10, TimeUnit.SECONDS);
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
        else if (payload instanceof RegisterCapteur req) {
            handleRegisterCapteur(req);
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
    
    /**
     * Convert a Map (from JSON deserialization) to a proper message object.
     * This handles remote messages that arrive as LinkedHashMap.
     */
    @SuppressWarnings("unchecked")
    protected Object convertMapToMessage(Map<?, ?> map) {
        System.out.println("[DEBUG] convertMapToMessage - Keys: " + map.keySet());
        
        // Check for CapteurDataUpdate (contains capteurId and reading)
        if (map.containsKey("capteurId") && map.containsKey("reading")) {
            System.out.println("[DEBUG] Found CapteurDataUpdate message");
            String capteurId = (String) map.get("capteurId");
            Object readingObj = map.get("reading");
            SensorReading reading = parseSensorReading(readingObj);
            if (reading != null) {
                return new CapteurDataUpdate(capteurId, reading);
            }
        }
        
        // Check for RegisterCapteur
        if (map.containsKey("capteurId") && map.containsKey("capteurType")) {
            System.out.println("[DEBUG] Found RegisterCapteur message");
            String capteurId = (String) map.get("capteurId");
            String capteurType = (String) map.get("capteurType");
            String kafkaTopic = (String) map.get("kafkaTopic");
            String location = (String) map.get("location");
            return new RegisterCapteur(capteurId, capteurType, kafkaTopic, location);
        }
        
        // Check if it contains a 'requester' field - likely RequestVilleInfo
        if (map.containsKey("requester")) {
            System.out.println("[DEBUG] Found 'requester' field - this is RequestVilleInfo");
            Object requesterObj = map.get("requester");
            if (requesterObj instanceof Map) {
                Map<String, Object> requesterMap = (Map<String, Object>) requesterObj;
                String reqActorId = (String) requesterMap.get("actorId");
                System.out.println("[DEBUG] Requester actorId: " + reqActorId);
                
                if (reqActorId != null && context != null) {
                    ActorRef requester = context.actorFor(reqActorId);
                    
                    if (requester == null && context instanceof DefaultActorContext) {
                        DefaultActorContext defaultContext = (DefaultActorContext) context;
                        RemoteMessageTransport transport = defaultContext.getRemoteTransport();
                        
                        if (transport != null) {
                            System.out.println("[DEBUG] Creating RemoteActorRefProxy for: " + reqActorId);
                            requester = new RemoteActorRefProxy(reqActorId, transport, context.self());
                        }
                    }
                    
                    if (requester != null) {
                        System.out.println("[DEBUG] Created RequestVilleInfo with requester: " + requester.getActorId());
                        return new RequestVilleInfo(requester);
                    }
                }
            }
        }
        
        // Check for RegisterClient - can have 'clientRef' or just 'clientId'
        if (map.containsKey("clientRef") || map.containsKey("clientId")) {
            System.out.println("[DEBUG] Found RegisterClient message");
            String clientActorId = null;
            
            // First try to get from clientRef object
            if (map.containsKey("clientRef")) {
                Object clientRefObj = map.get("clientRef");
                if (clientRefObj instanceof Map) {
                    Map<String, Object> clientRefMap = (Map<String, Object>) clientRefObj;
                    clientActorId = (String) clientRefMap.get("actorId");
                }
            }
            
            // Fallback to clientId field directly
            if (clientActorId == null && map.containsKey("clientId")) {
                clientActorId = (String) map.get("clientId");
            }
            
            System.out.println("[DEBUG] ClientRef actorId: " + clientActorId);
            
            if (clientActorId != null && context != null) {
                ActorRef clientRef = context.actorFor(clientActorId);
                
                if (clientRef == null && context instanceof DefaultActorContext) {
                    DefaultActorContext defaultContext = (DefaultActorContext) context;
                    RemoteMessageTransport transport = defaultContext.getRemoteTransport();
                    
                    if (transport != null) {
                        System.out.println("[DEBUG] Creating RemoteActorRefProxy for client: " + clientActorId);
                        clientRef = new RemoteActorRefProxy(clientActorId, transport, context.self());
                    }
                }
                
                if (clientRef != null) {
                    System.out.println("[DEBUG] Created RegisterClient with clientRef: " + clientRef.getActorId());
                    return new RegisterClient(clientRef);
                }
            } else if (clientActorId != null) {
                // Create RegisterClient with just the clientId (for REST API calls without context)
                System.out.println("[DEBUG] Creating RegisterClient with clientId only: " + clientActorId);
                RegisterClient msg = new RegisterClient();
                msg.setClientId(clientActorId);
                return msg;
            }
        }
        
        // Check for nested payload structure
        if (map.containsKey("payload") && map.get("payload") instanceof Map) {
            System.out.println("[DEBUG] Found nested payload, unwrapping...");
            Map<String, Object> innerPayload = (Map<String, Object>) map.get("payload");
            return convertMapToMessage(innerPayload);
        }
        
        System.out.println("[DEBUG] Could not convert map, returning as-is");
        return map;
    }
    
    /**
     * Parse a SensorReading from various input formats.
     */
    @SuppressWarnings("unchecked")
    private SensorReading parseSensorReading(Object readingObj) {
        if (readingObj == null) {
            return null;
        }
        
        if (readingObj instanceof SensorReading) {
            return (SensorReading) readingObj;
        }
        
        if (readingObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) readingObj;
            String sensorType = (String) map.get("sensorType");
            double value = getDoubleValue(map, "value", 0.0);
            long timestamp = map.containsKey("timestamp") ? 
                ((Number) map.get("timestamp")).longValue() : 
                System.currentTimeMillis();
            
            if (sensorType != null) {
                return new SensorReading(sensorType, value, timestamp);
            }
        }
        
        return null;
    }
    
    private void handleRegisterClient(RegisterClient req) {
        if (req.getClientRef() != null) {
            registeredClients.put(req.getClientRef().getActorId(), req.getClientRef());
            System.out.println("Client registered with " + name + ": " + req.getClientRef().getActorId());
        } else if (req.getClientId() != null) {
            registeredClients.put(req.getClientId(), new DummyActorRef(req.getClientId()));
            System.out.println("Client registered with " + name + ": " + req.getClientId());
        }
    }
    
    private void handleUnregisterClient(UnregisterClient req) {
        if (req.getClientRef() != null) {
            registeredClients.remove(req.getClientRef().getActorId());
            System.out.println("Client unregistered from " + name + ": " + req.getClientRef().getActorId());
        } else if (req.getClientId() != null) {
            registeredClients.remove(req.getClientId());
            System.out.println("Client unregistered from " + name + ": " + req.getClientId());
        }
    }
    
    /**
     * Handle registration of a new sensor (capteur) to this city.
     * Called when a sensor is associated with this city via message.
     */
    protected void handleRegisterCapteur(RegisterCapteur req) {
        CapteurInfo info = new CapteurInfo(
            req.getCapteurId(),
            req.getCapteurType(),
            "ACTIVE",
            actorId  // This city's ID
        );
        registeredCapteurs.put(req.getCapteurId(), info);
        System.out.println(name + " registered capteur: " + req.getCapteurId() + 
                          " (type=" + req.getCapteurType() + ", topic=" + req.getKafkaTopic() + 
                          ", location=" + req.getLocation() + ")");
        
        // Subclasses (HttpVilleActor) can override to subscribe to Kafka topic
        onCapteurRegistered(req);
    }
    
    /**
     * Hook for subclasses to perform additional actions when a capteur is registered.
     * HttpVilleActor overrides this to subscribe to the Kafka topic.
     */
    protected void onCapteurRegistered(RegisterCapteur req) {
        // Default implementation does nothing
        // HttpVilleActor overrides this to subscribe to Kafka topic
    }
    
    protected void handleCapteurDataUpdate(CapteurDataUpdate update) {
        latestReadings.put(update.getCapteurId(), update.getReading());
        System.out.println(name + " received sensor reading from " + update.getCapteurId());
    }
    
    private void handleWeatherRequest(WeatherRequest req) {
        System.out.println(name + " received weather request from " + req.getClientId());
        
        Map<String, Double> aggregated = aggregateSensorData();
        
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
                             ", capteurs=" + registeredCapteurs.size() +
                             ", sensors=" + latestReadings.size());
        }
    }
    
    /**
     * Broadcast climate report to registered clients.
     * Called every 5 seconds by scheduler.
     */
    protected void broadcastClimateReport() {
        if (!"ACTIVE".equals(status)) {
            return;
        }
        
        Map<String, Double> aggregated = aggregateSensorData();
        
        ClimateReport report = new ClimateReport(
            context != null ? context.self().getActorId() : "unknown",
            name,
            aggregated,
            latestReadings.size(),
            System.currentTimeMillis()
        );
        
        // Subclasses can override to publish to Kafka
        onBroadcastClimateReport(report);
    }
    
    /**
     * Hook for subclasses to publish climate reports.
     * HttpVilleActor overrides this to publish to Kafka.
     */
    protected void onBroadcastClimateReport(ClimateReport report) {
        // Default: just log
        System.out.println(name + " climate report: " + aggregateSensorData());
    }
    
    @Override
    public void postStop() {
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
        
        for (Map.Entry<String, List<Double>> entry : groupedByType.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            aggregated.put(entry.getKey(), avg);
        }
        
        return aggregated;
    }
    
    public Map<String, SensorReading> getLatestReadings() {
        return latestReadings;
    }
    
    public Map<String, CapteurInfo> getRegisteredCapteurs() {
        return registeredCapteurs;
    }
    
    public String getName() {
        return name;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public int getRegisteredClientsCount() {
        return registeredClients.size();
    }
    
    /**
     * Get the map of registered clients.
     * Protected access for subclasses (like HttpVilleActor).
     */
    protected Map<String, ActorRef> getRegisteredClients() {
        return registeredClients;
    }

    public ClimateConfig getClimateConfig() {
        return climateConfig;
    }

    /**
     * DummyActorRef for REST API testing
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
        public void tell(Message message) {}

        @Override
        public void tell(Message message, ActorRef sender) {}

        @Override
        public java.util.concurrent.CompletableFuture<Object> ask(Message message, long timeout, java.util.concurrent.TimeUnit unit) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("DummyActorRef doesn't support ask pattern"));
        }

        @Override
        public void forward(Message message, ActorRef originalSender) {}

        @Override
        public boolean isActive() { return true; }

        @Override
        public void stop() {}

        @Override
        public void block() {}

        @Override
        public void unblock() {}

        @Override
        public void restart(Throwable cause) {}

        @Override
        public ActorLifecycleState getState() {
            return ActorLifecycleState.RUNNING;
        }

        @Override
        public void watch(ActorRef watcher) {}

        @Override
        public void unwatch(ActorRef watcher) {}

        @Override
        public String toString() {
            return "DummyActorRef(" + actorId + ")";
        }
    }
}
