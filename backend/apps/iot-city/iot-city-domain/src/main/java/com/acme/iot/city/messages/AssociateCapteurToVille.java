package com.acme.iot.city.messages;

import com.acme.iot.city.model.ClimateConfig;

/**
 * Message sent from a VilleActor to a CapteurActor to inform it of its association.
 * This enables the capteur to know which ville it should send readings to.
 */
public class AssociateCapteurToVille {
    
    private final String villeId;
    private final String villeName;
    private final ClimateConfig climateConfig;
    
    public AssociateCapteurToVille(String villeId, String villeName, ClimateConfig climateConfig) {
        this.villeId = villeId;
        this.villeName = villeName;
        this.climateConfig = climateConfig;
    }
    
    public String getVilleId() {
        return villeId;
    }
    
    public String getVilleName() {
        return villeName;
    }
    
    public ClimateConfig getClimateConfig() {
        return climateConfig;
    }
    
    @Override
    public String toString() {
        return "AssociateCapteurToVille{villeId='" + villeId + "', villeName='" + villeName + "'}";
    }
}
