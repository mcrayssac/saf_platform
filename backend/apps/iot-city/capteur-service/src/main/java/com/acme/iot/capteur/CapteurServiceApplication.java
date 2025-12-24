package com.acme.iot.capteur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Capteur Service Application
 * 
 * Microservice responsible for managing CapteurActor instances.
 * Each CapteurActor represents a sensor in the IoT City system.
 * 
 * Port: 8086
 */
@SpringBootApplication
@EnableScheduling
public class CapteurServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapteurServiceApplication.class, args);
    }
}
