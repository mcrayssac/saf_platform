package com.acme.iot.city.messages;

import com.acme.iot.city.model.SensorReading;

/**
 * Internal message - sent from CapteurActor to VilleActor with sensor readings.
 * VilleActor aggregates these readings for climate reports.
 */
public class CapteurDataUpdate {
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
    public String toString() {
        return "CapteurDataUpdate{" +
                "capteurId='" + capteurId + '\'' +
                ", reading=" + reading +
                '}';
    }
}
