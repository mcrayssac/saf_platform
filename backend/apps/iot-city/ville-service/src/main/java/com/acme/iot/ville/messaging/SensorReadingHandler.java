package com.acme.iot.ville.messaging;

import com.acme.iot.city.messages.CapteurDataUpdate;
import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.SimpleMessage;
import com.acme.saf.saf_runtime.messaging.InterPodMessaging;
import com.acme.saf.saf_runtime.messaging.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles incoming sensor readings from Kafka and dispatches them to VilleActors.
 * 
 * This handler subscribes to the "iot-city-sensor-readings" topic and broadcasts
 * the sensor data to all VilleActors in the system.
 */
public class SensorReadingHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SensorReadingHandler.class);
    
    private static final String SENSOR_READINGS_TOPIC = "iot-city-sensor-readings";
    
    private final ActorSystem actorSystem;
    private final InterPodMessaging messaging;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor;
    
    public SensorReadingHandler(ActorSystem actorSystem, InterPodMessaging messaging) {
        this.actorSystem = actorSystem;
        this.messaging = messaging;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sensor-reading-handler");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start listening for sensor readings on Kafka.
     */
    public void start() {
        if (running.getAndSet(true)) {
            logger.warn("SensorReadingHandler is already running");
            return;
        }
        
        try {
            MessageConsumer consumer = messaging.getConsumer();
            
            // Subscribe to SensorReading messages
            consumer.subscribe(
                SensorReading.class.getName(),
                SensorReading.class,
                this::handleSensorReading,
                this::handleError
            );
            
            // Start listening on the topic
            consumer.listen(SENSOR_READINGS_TOPIC);
            
            logger.info("SensorReadingHandler started listening on topic: {}", SENSOR_READINGS_TOPIC);
            
        } catch (Exception e) {
            running.set(false);
            logger.error("Failed to start SensorReadingHandler", e);
            throw new RuntimeException("Failed to start SensorReadingHandler", e);
        }
    }
    
    /**
     * Handle an incoming SensorReading and dispatch to VilleActors.
     */
    private void handleSensorReading(SensorReading reading) {
        if (reading == null) {
            logger.warn("Received null SensorReading");
            return;
        }
        
        logger.info("Received SensorReading via Kafka: type={}, value={}", 
                   reading.getSensorType(), 
                   String.format("%.2f", reading.getValue()));
        
        // Get all actor IDs and dispatch to each
        List<String> actorIds = actorSystem.getAllActorIds();
        
        for (String actorId : actorIds) {
            try {
                ActorRef actor = actorSystem.getActor(actorId);
                if (actor != null) {
                    // Wrap in CapteurDataUpdate message
                    CapteurDataUpdate update = new CapteurDataUpdate("remote-capteur", reading);
                    actor.tell(new SimpleMessage(update));
                    logger.debug("Dispatched SensorReading to actor: {}", actorId);
                }
            } catch (Exception e) {
                logger.error("Failed to dispatch SensorReading to actor: {}", actorId, e);
            }
        }
    }
    
    /**
     * Handle errors during message processing.
     */
    private void handleError(Exception error) {
        logger.error("Error processing Kafka sensor reading", error);
    }
    
    /**
     * Stop listening for sensor readings.
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        
        try {
            messaging.getConsumer().stopListening(SENSOR_READINGS_TOPIC);
            executor.shutdownNow();
            logger.info("SensorReadingHandler stopped");
        } catch (Exception e) {
            logger.error("Error stopping SensorReadingHandler", e);
        }
    }
    
    public boolean isRunning() {
        return running.get();
    }
}
