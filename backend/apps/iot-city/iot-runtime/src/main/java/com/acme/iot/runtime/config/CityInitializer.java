package com.acme.iot.runtime.config;

import com.acme.iot.city.messages.ClimateConfigUpdate;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.SimpleMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Application component that creates pre-configured cities on startup.
 * Runs after Spring Boot application has started.
 */
@Component
public class CityInitializer implements ApplicationRunner {
    
    @Autowired
    private ActorSystem actorSystem;
    
    @Autowired
    private CityConfiguration cityConfig;
    
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("=".repeat(60));
        System.out.println("=== Initializing Pre-configured Cities ===");
        System.out.println("=".repeat(60));
        
        for (CityConfiguration.PredefinedCity city : cityConfig.getPredefined()) {
            createVilleWithSensors(city);
        }
        
        System.out.println("=".repeat(60));
        System.out.println("=== City Initialization Complete ===");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Create a ville actor with its associated sensors.
     */
    private void createVilleWithSensors(CityConfiguration.PredefinedCity city) {
        try {
            // Create ville actor
            Map<String, Object> villeParams = new HashMap<>();
            villeParams.put("name", city.getName());
            villeParams.put("climateConfig", city.getClimate().toClimateConfig());
            
            ActorRef ville = actorSystem.spawn("VILLE", villeParams);
            System.out.println("✓ Created ville: " + city.getName() + " (ID: " + ville.getActorId() + ")");
            
            // Create sensors for this ville
            createSensorsForVille(ville, city);
            
        } catch (Exception e) {
            System.err.println("✗ Failed to create ville: " + city.getName());
            e.printStackTrace();
        }
    }
    
    /**
     * Create temperature, pressure, and humidity sensors for a ville.
     */
    private void createSensorsForVille(ActorRef villeRef, CityConfiguration.PredefinedCity city) {
        String[] sensorTypes = {"TEMPERATURE", "PRESSURE", "HUMIDITY"};
        
        for (String type : sensorTypes) {
            try {
                // Create capteur actor
                Map<String, Object> capteurParams = new HashMap<>();
                capteurParams.put("type", type);
                
                ActorRef capteur = actorSystem.spawn("CAPTEUR", capteurParams);
                
                // Send climate config to capteur
                ClimateConfigUpdate configMsg = new ClimateConfigUpdate(
                    city.getClimate().toClimateConfig()
                );
                capteur.tell(new SimpleMessage(configMsg), null);
                
                // Associate capteur with ville using message passing
                // Send a string command that CapteurActor will handle to set association
                capteur.tell(new SimpleMessage("ASSOCIATE:" + villeRef.getActorId()), null);
                
                System.out.println("  ✓ Created sensor: " + type + " for " + city.getName());
                
            } catch (Exception e) {
                System.err.println("  ✗ Failed to create sensor: " + type + " for " + city.getName());
                e.printStackTrace();
            }
        }
    }
}
