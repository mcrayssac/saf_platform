package com.acme.iot.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Client Service Application
 * 
 * Microservice responsible for managing ClientActor instances.
 * Each ClientActor represents a user connected to the IoT City system.
 * 
 * Port: 8084
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {
        "com.acme.iot.client",
        "com.acme.saf.saf_runtime"
    },
    excludeFilters = @ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = com.acme.saf.saf_runtime.DefaultActorSystem.class
    )
)
public class ClientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientServiceApplication.class, args);
    }
}
