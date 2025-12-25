package com.acme.iot.city.messages;

import com.acme.iot.city.model.SensorReading;
import com.acme.saf.actor.core.Message;

import java.time.Instant;

/**
 * Internal message - sent from CapteurActor to VilleActor with sensor readings.
 * VilleActor aggregates these readings for climate reports.
 */
public class CapteurDataUpdate implements Message {
    private String capteurId;
    private SensorReading reading;
    
    public CapteurDataUpdate() {
        // Default constructor for Jackson deserialization
    }
    
    public CapteurDataUpdate(String capteurId, SensorReading reading) {
        this.capteurId = capteurId;
        this.reading = reading;
    }
    
    public String getCapteurId() {
        return capteurId;
    }
    
    public void setCapteurId(String capteurId) {
        this.capteurId = capteurId;
    }
    
    public SensorReading getReading() {
        return reading;
    }
    
    public void setReading(SensorReading reading) {
        this.reading = reading;
    }
    
    @Override
    public Object getPayload() {
        return this.reading;
    }
    
    @Override
    public Instant getTimestamp() {
        // Convert the reading's timestamp (long ms) to Instant
        if (reading != null && reading.getTimestamp() > 0) {
            return Instant.ofEpochMilli(reading.getTimestamp());
        }
        return Instant.now();
    }
    
    @Override
    public String toString() {
        return "CapteurDataUpdate{" +
                "capteurId='" + capteurId + '\'' +
                ", reading=" + reading +
                '}';
    }
}
