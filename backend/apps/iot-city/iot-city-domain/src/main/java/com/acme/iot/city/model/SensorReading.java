package com.acme.iot.city.model;

/**
 * Represents a sensor reading from a capteur.
 * Application-specific model.
 */
public class SensorReading {
    private String sensorType;  // "TEMPERATURE", "PRESSURE", "HUMIDITY"
    private double value;
    private long timestamp;
    
    public SensorReading() {
    }
    
    public SensorReading(String sensorType, double value, long timestamp) {
        this.sensorType = sensorType;
        this.value = value;
        this.timestamp = timestamp;
    }
    
    public String getSensorType() {
        return sensorType;
    }
    
    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "SensorReading{" +
                "sensorType='" + sensorType + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
