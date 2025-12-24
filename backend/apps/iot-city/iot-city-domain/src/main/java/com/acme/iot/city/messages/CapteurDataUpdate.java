package com.acme.iot.city.messages;

import com.acme.iot.city.model.SensorReading;

/**
 * Internal message - sent from CapteurActor to VilleActor with sensor readings.
 * VilleActor aggregates these readings for climate reports.
 */
public class CapteurDataUpdate {
    private final String capteurId;
    private final SensorReading reading;
    
    public CapteurDataUpdate(String capteurId, SensorReading reading) {
        this.capteurId = capteurId;
        this.reading = reading;
    }
    
    public String getCapteurId() {
        return capteurId;
    }
    
    public SensorReading getReading() {
        return reading;
    }
    
    @Override
    public String toString() {
        return "CapteurDataUpdate{" +
                "capteurId='" + capteurId + '\'' +
                ", reading=" + reading +
                '}';
    }
}
