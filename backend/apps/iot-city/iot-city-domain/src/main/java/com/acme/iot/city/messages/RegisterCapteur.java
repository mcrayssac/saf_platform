package com.acme.iot.city.messages;

import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Message to register a sensor (capteur) with a city (ville).
 * This allows dynamic association of sensors to cities after both are created.
 */
@JsonTypeName("RegisterCapteur")
public class RegisterCapteur implements Message {
    private static final long serialVersionUID = 1L;
    
    private String capteurId;      // ID of the sensor actor
    private String capteurType;    // TEMPERATURE, HUMIDITY, PRESSURE
    private String kafkaTopic;     // Topic where the sensor publishes readings
    private String location;       // Physical location description
    
    public RegisterCapteur() {
    }
    
    public RegisterCapteur(String capteurId, String capteurType, String kafkaTopic, String location) {
        this.capteurId = capteurId;
        this.capteurType = capteurType;
        this.kafkaTopic = kafkaTopic;
        this.location = location;
    }
    
    public String getCapteurId() {
        return capteurId;
    }
    
    public void setCapteurId(String capteurId) {
        this.capteurId = capteurId;
    }
    
    public String getCapteurType() {
        return capteurType;
    }
    
    public void setCapteurType(String capteurType) {
        this.capteurType = capteurType;
    }
    
    public String getKafkaTopic() {
        return kafkaTopic;
    }
    
    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @Override
    public Object getPayload() {
        return this;
    }
    
    @Override
    public String toString() {
        return "RegisterCapteur{capteurId='" + capteurId + "', capteurType='" + capteurType + 
               "', kafkaTopic='" + kafkaTopic + "', location='" + location + "'}";
    }
}
