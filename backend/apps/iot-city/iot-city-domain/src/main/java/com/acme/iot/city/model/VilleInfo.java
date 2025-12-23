package com.acme.iot.city.model;

/**
 * Summary information about a city.
 * Application-specific model.
 */
public class VilleInfo {
    private String villeId;
    private String name;
    private String status;  // "ACTIVE", "INACTIVE"
    private ClimateConfig climateConfig;
    private int capteursCount;
    
    public VilleInfo() {
    }
    
    public VilleInfo(String villeId, String name, String status, 
                    ClimateConfig climateConfig, int capteursCount) {
        this.villeId = villeId;
        this.name = name;
        this.status = status;
        this.climateConfig = climateConfig;
        this.capteursCount = capteursCount;
    }
    
    public String getVilleId() {
        return villeId;
    }
    
    public void setVilleId(String villeId) {
        this.villeId = villeId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public ClimateConfig getClimateConfig() {
        return climateConfig;
    }
    
    public void setClimateConfig(ClimateConfig climateConfig) {
        this.climateConfig = climateConfig;
    }
    
    public int getCapteursCount() {
        return capteursCount;
    }
    
    public void setCapteursCount(int capteursCount) {
        this.capteursCount = capteursCount;
    }
    
    @Override
    public String toString() {
        return "VilleInfo{" +
                "villeId='" + villeId + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", climateConfig=" + climateConfig +
                ", capteursCount=" + capteursCount +
                '}';
    }
}
