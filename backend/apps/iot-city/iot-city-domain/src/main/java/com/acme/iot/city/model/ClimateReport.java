package com.acme.iot.city.model;

import java.util.Map;

/**
 * Aggregated climate data from a city.
 * Sent to clients via WebSocket every 5 seconds.
 * Application-specific model.
 */
public class ClimateReport {
    private String villeId;
    private String villeName;
    private Map<String, Double> aggregatedData;  // sensorType -> avg value
    private int activeCapteurs;
    private long timestamp;
    
    public ClimateReport() {
    }
    
    public ClimateReport(String villeId, String villeName, 
                        Map<String, Double> aggregatedData, 
                        int activeCapteurs, long timestamp) {
        this.villeId = villeId;
        this.villeName = villeName;
        this.aggregatedData = aggregatedData;
        this.activeCapteurs = activeCapteurs;
        this.timestamp = timestamp;
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
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "ClimateReport{" +
                "villeId='" + villeId + '\'' +
                ", villeName='" + villeName + '\'' +
                ", aggregatedData=" + aggregatedData +
                ", activeCapteurs=" + activeCapteurs +
                ", timestamp=" + timestamp +
                '}';
    }
}
