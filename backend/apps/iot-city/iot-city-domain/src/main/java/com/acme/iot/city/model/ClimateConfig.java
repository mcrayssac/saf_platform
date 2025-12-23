package com.acme.iot.city.model;

/**
 * Configuration for a city's expected climate.
 * Application-specific model.
 */
public class ClimateConfig {
    private double meanTemperature;      // °C, e.g., 20.0
    private double meanPressure;         // hPa, e.g., 1013.25
    private double meanHumidity;         // %, e.g., 60.0
    private double variancePercentage;   // %, e.g., 10.0 (±10% random)
    
    public ClimateConfig() {
    }
    
    public ClimateConfig(double meanTemperature, double meanPressure, 
                        double meanHumidity, double variancePercentage) {
        this.meanTemperature = meanTemperature;
        this.meanPressure = meanPressure;
        this.meanHumidity = meanHumidity;
        this.variancePercentage = variancePercentage;
    }
    
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
    
    @Override
    public String toString() {
        return "ClimateConfig{" +
                "meanTemperature=" + meanTemperature +
                ", meanPressure=" + meanPressure +
                ", meanHumidity=" + meanHumidity +
                ", variancePercentage=" + variancePercentage +
                '}';
    }
}
