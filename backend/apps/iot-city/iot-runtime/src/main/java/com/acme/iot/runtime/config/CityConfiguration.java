package com.acme.iot.runtime.config;

import com.acme.iot.city.model.ClimateConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for pre-defined cities.
 * Binds to iot.cities.predefined in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "iot.cities")
public class CityConfiguration {
    
    private List<PredefinedCity> predefined = new ArrayList<>();
    
    public List<PredefinedCity> getPredefined() {
        return predefined;
    }
    
    public void setPredefined(List<PredefinedCity> predefined) {
        this.predefined = predefined;
    }
    
    /**
     * Represents a pre-configured city from application.yml
     */
    public static class PredefinedCity {
        private String name;
        private ClimateConfigData climate;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public ClimateConfigData getClimate() {
            return climate;
        }
        
        public void setClimate(ClimateConfigData climate) {
            this.climate = climate;
        }
    }
    
    /**
     * Climate configuration data from YAML.
     * Converts to domain ClimateConfig object.
     */
    public static class ClimateConfigData {
        private double meanTemperature;
        private double meanPressure;
        private double meanHumidity;
        private double variancePercentage;
        
        public double getMeanTemperature() {
            return meanTemperature;
        }
        
        public void setMeanTemperature(double meanTemperature) {
            this.meanTemperature = meanTemperature;
        }
        
        public double getMeanPressure() {
            return meanPressure;
        }
        
        public void setMeanPressure(double meanPressure) {
            this.meanPressure = meanPressure;
        }
        
        public double getMeanHumidity() {
            return meanHumidity;
        }
        
        public void setMeanHumidity(double meanHumidity) {
            this.meanHumidity = meanHumidity;
        }
        
        public double getVariancePercentage() {
            return variancePercentage;
        }
        
        public void setVariancePercentage(double variancePercentage) {
            this.variancePercentage = variancePercentage;
        }
        
        /**
         * Convert to domain ClimateConfig object.
         */
        public ClimateConfig toClimateConfig() {
            return new ClimateConfig(
                meanTemperature,
                meanPressure,
                meanHumidity,
                variancePercentage
            );
        }
    }
}
