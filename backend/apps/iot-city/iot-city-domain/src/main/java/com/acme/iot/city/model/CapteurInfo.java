package com.acme.iot.city.model;

/**
 * Summary information about a sensor (capteur).
 * Application-specific model.
 */
public class CapteurInfo {
    private String capteurId;
    private String type;  // "TEMPERATURE", "PRESSURE", "HUMIDITY"
    private String status;  // "ACTIVE", "INACTIVE"
    private String associatedVilleId;
    
    public CapteurInfo() {
    }
    
    public CapteurInfo(String capteurId, String type, String status, String associatedVilleId) {
        this.capteurId = capteurId;
        this.type = type;
        this.status = status;
        this.associatedVilleId = associatedVilleId;
    }
    
    public String getCapteurId() {
        return capteurId;
    }
    
    public void setCapteurId(String capteurId) {
        this.capteurId = capteurId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getAssociatedVilleId() {
        return associatedVilleId;
    }
    
    public void setAssociatedVilleId(String associatedVilleId) {
        this.associatedVilleId = associatedVilleId;
    }
    
    @Override
    public String toString() {
        return "CapteurInfo{" +
                "capteurId='" + capteurId + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", associatedVilleId='" + associatedVilleId + '\'' +
                '}';
    }
}
