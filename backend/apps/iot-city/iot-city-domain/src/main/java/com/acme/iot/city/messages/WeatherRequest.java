package com.acme.iot.city.messages;

import com.acme.saf.actor.core.Message;
import java.time.Instant;

/**
 * Request message sent to VilleActor to get current climate data.
 * VilleActor should respond with a ClimateReport.
 */
public class WeatherRequest implements Message {
    private String villeId;
    private String clientId;
    private Instant timestamp;
    
    public WeatherRequest() {
        this.timestamp = Instant.now();
    }
    
    public WeatherRequest(String villeId, String clientId) {
        this.villeId = villeId;
        this.clientId = clientId;
        this.timestamp = Instant.now();
    }
    
    public String getVilleId() {
        return villeId;
    }
    
    public void setVilleId(String villeId) {
        this.villeId = villeId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public Object getPayload() {
        return this;
    }
    
    @Override
    public String toString() {
        return "WeatherRequest{" +
                "villeId='" + villeId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
