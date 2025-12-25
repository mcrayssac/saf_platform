package com.acme.iot.city.model;

import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregated climate data from a city.
 * Sent to clients via WebSocket every 5 seconds.
 * Application-specific model.
 */
public class ClimateReport implements Message {
    private String villeId;
    private String villeName;
    private Map<String, Double> aggregatedData;  // sensorType -> avg value
    private int activeCapteurs;
    private long timestampMillis;
    
    // Message interface fields
    private String messageId = UUID.randomUUID().toString();
    private String correlationId;
    
    public ClimateReport() {
    }
    
    public ClimateReport(String villeId, String villeName, 
                        Map<String, Double> aggregatedData, 
                        int activeCapteurs, long timestamp) {
        this.villeId = villeId;
        this.villeName = villeName;
        this.aggregatedData = aggregatedData;
        this.activeCapteurs = activeCapteurs;
        this.timestampMillis = timestamp;
    }
    
    public String getVilleId() {
        return villeId;
    }
    
    public void setVilleId(String villeId) {
        this.villeId = villeId;
    }
    
    public String getVilleName() {
        return villeName;
    }
    
    public void setVilleName(String villeName) {
        this.villeName = villeName;
    }
    
    public Map<String, Double> getAggregatedData() {
        return aggregatedData;
    }
    
    public void setAggregatedData(Map<String, Double> aggregatedData) {
        this.aggregatedData = aggregatedData;
    }
    
    public int getActiveCapteurs() {
        return activeCapteurs;
    }
    
    public void setActiveCapteurs(int activeCapteurs) {
        this.activeCapteurs = activeCapteurs;
    }
    
    public long getTimestampMillis() {
        return timestampMillis;
    }
    
    public void setTimestampMillis(long timestamp) {
        this.timestampMillis = timestamp;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    @Override
    public String toString() {
        return "ClimateReport{" +
                "villeId='" + villeId + '\'' +
                ", villeName='" + villeName + '\'' +
                ", aggregatedData=" + aggregatedData +
                ", activeCapteurs=" + activeCapteurs +
                ", timestamp=" + timestampMillis +
                '}';
    }
    
    // Message interface implementation
    @Override
    @JsonIgnore
    public String getMessageId() {
        return messageId;
    }
    
    @Override
    @JsonIgnore
    public Instant getTimestamp() {
        return Instant.ofEpochMilli(timestampMillis);
    }
    
    @Override
    @JsonIgnore
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    @JsonIgnore
    public Object getPayload() {
        return this;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
