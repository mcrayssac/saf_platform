package com.acme.iot.ville.messaging;

import com.acme.iot.city.model.ClimateReport;
import com.acme.saf.saf_runtime.messaging.InterPodMessaging;
import com.acme.saf.saf_runtime.messaging.MessageConsumer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a client's subscription to a city's weather topic on Kafka.
 * 
 * When a client registers with a city, a ClientSubscription is created which:
 * - Subscribes to the city's weather Kafka topic
 * - Listens for climate report updates
 * - Caches the latest report for retrieval via REST API
 */
@Slf4j
public class ClientSubscription {
    
    private final String clientId;
    private final String villeId;
    private final String weatherTopicName;
    private final MessageConsumer consumer;
    private volatile ClimateReport latestReport;
    private volatile boolean active = true;
    
    public ClientSubscription(String clientId, String villeId, InterPodMessaging interPodMessaging) throws Exception {
        this.clientId = clientId;
        this.villeId = villeId;
        this.weatherTopicName = "ville-" + villeId + "-weather";
        
        // Get the consumer from InterPodMessaging
        this.consumer = interPodMessaging.getConsumer();
        
        // Subscribe to the city's weather topic
        consumer.subscribe(
            ClimateReport.class.getName(),
            ClimateReport.class,
            report -> {
                this.latestReport = report;
                log.debug("Client {} received climate report for {}: {} sensors, temp={}", 
                    clientId, villeId, report.getActiveCapteurs(), 
                    report.getAggregatedData().get("temperature"));
            },
            exception -> {
                log.error("Client {} subscription error for topic {}: {}", 
                    clientId, weatherTopicName, exception.getMessage());
            }
        );
        
        // Start listening to the weather topic
        consumer.listen(this.weatherTopicName);
        log.info("Client {} subscribed to weather topic: {}", clientId, weatherTopicName);
    }
    
    /**
     * Get the latest climate report received from Kafka.
     */
    public ClimateReport getLatestReport() {
        return latestReport;
    }
    
    /**
     * Check if this subscription is still active.
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Unsubscribe from the topic and clean up resources.
     */
    public void close() throws Exception {
        if (!active) {
            return;
        }
        
        active = false;
        try {
            consumer.stopListening(weatherTopicName);
            log.info("Client {} unsubscribed from weather topic: {}", clientId, weatherTopicName);
        } catch (Exception e) {
            log.error("Error unsubscribing client {} from topic {}: {}", 
                clientId, weatherTopicName, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public String toString() {
        return "ClientSubscription{" +
                "clientId='" + clientId + '\'' +
                ", villeId='" + villeId + '\'' +
                ", topic='" + weatherTopicName + '\'' +
                ", active=" + active +
                ", hasReport=" + (latestReport != null) +
                '}';
    }
}
