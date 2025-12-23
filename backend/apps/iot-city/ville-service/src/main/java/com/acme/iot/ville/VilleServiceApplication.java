package com.acme.iot.ville;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ville Microservice Application.
 * 
 * This microservice manages VilleActor instances.
 * Each ville (city) is an actor that:
 * - Registers/unregisters clients for climate updates
 * - Receives sensor data from CapteurActors
 * - Aggregates climate data and sends reports to clients
 */
@SpringBootApplication
public class VilleServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VilleServiceApplication.class, args);
    }
}
