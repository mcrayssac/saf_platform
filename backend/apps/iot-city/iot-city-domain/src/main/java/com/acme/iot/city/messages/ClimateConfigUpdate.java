package com.acme.iot.city.messages;

import com.acme.iot.city.model.ClimateConfig;

/**
 * Internal message - sent from VilleActor to CapteurActor with climate configuration.
 * CapteurActor uses this to generate sensor readings with appropriate variance.
 */
public class ClimateConfigUpdate {
    private final ClimateConfig config;
    
    public ClimateConfigUpdate(ClimateConfig config) {
        this.config = config;
    }
    
    public ClimateConfig getConfig() {
        return config;
    }
    
    @Override
    public String toString() {
        return "ClimateConfigUpdate{" +
                "config=" + config +
                '}';
    }
}
