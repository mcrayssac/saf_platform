package com.acme.iot.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Client Service Application
 * 
 * Microservice responsible for managing ClientActor instances.
 * Each ClientActor represents a user connected to the IoT City system.
 * 
 * Port: 8082
 */
@SpringBootApplication
public class ClientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientServiceApplication.class, args);
    }
}
