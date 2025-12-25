package com.acme.iot.ville.controller;

import com.acme.iot.city.actors.VilleActor;
import com.acme.iot.city.model.SensorReading;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Weather API endpoint for retrieving current climate data for a city.
 * Returns aggregated sensor readings from all sensors associated with the city.
 * 
 * GET /weather/{villeId} - Get current weather for a city
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    // In-memory cache of VilleActor instances - public for access from other controllers
    public static final Map<String, VilleActor> villeActorCache = new ConcurrentHashMap<>();

    /**
     * Get current weather data for a city.
     */
    @GetMapping("/{villeId}")
    public Map<String, Object> getWeather(@PathVariable String villeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            VilleActor villeActor = villeActorCache.get(villeId);
            
            if (villeActor == null) {
                response.put("error", "City not found: " + villeId);
                response.put("status", "CITY_NOT_FOUND");
                return response;
            }
            
            Map<String, SensorReading> readings = villeActor.getLatestReadings();
            
            // Add test data if no real sensors
            if (readings.isEmpty()) {
                // Simulate some sensors for demo
                readings = new HashMap<>();
                readings.put("temp-sensor-1", new SensorReading("temperature", 22.5, System.currentTimeMillis()));
                readings.put("humidity-sensor-1", new SensorReading("humidity", 65.0, System.currentTimeMillis()));
                readings.put("pressure-sensor-1", new SensorReading("pressure", 1013.25, System.currentTimeMillis()));
            }
            
            Map<String, Double> aggregated = aggregateSensorData(readings);
            
            response.put("villeId", villeId);
            response.put("villeName", villeActor.getName());
            response.put("data", aggregated);
            response.put("sensorCount", readings.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", readings.isEmpty() ? "DEMO_DATA" : "SUCCESS");
            
        } catch (Exception e) {
            response.put("error", "Failed to retrieve weather: " + e.getMessage());
            response.put("status", "ERROR");
        }
        
        return response;
    }

    private Map<String, Double> aggregateSensorData(Map<String, SensorReading> readings) {
        Map<String, Double> aggregated = new HashMap<>();
        Map<String, Double> sums = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        
        for (SensorReading reading : readings.values()) {
            String type = reading.getSensorType();
            Double value = reading.getValue();
            
            sums.put(type, sums.getOrDefault(type, 0.0) + value);
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        
        for (String type : sums.keySet()) {
            double avg = sums.get(type) / counts.get(type);
            aggregated.put(type, Math.round(avg * 100.0) / 100.0);
        }
        
        return aggregated;
    }

    public static void registerVilleActor(String villeId, VilleActor villeActor) {
        villeActorCache.put(villeId, villeActor);
        System.out.println("VilleActor registered: " + villeId);
    }
}
