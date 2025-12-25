package com.acme.iot.ville.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.protocol.ActorCreatedResponse;
import com.acme.saf.actor.core.protocol.CreateActorCommand;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.ville.actor.HttpVilleActorFactory;
import com.acme.iot.city.actors.VilleActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor runtime controller for ville-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 * VilleActors are automatically registered with WeatherController by the factory.
 * 
 * When a VilleActor is created, 3 CapteurActors are automatically spawned:
 * - temperature sensor
 * - humidity sensor
 * - pressure sensor
 */
@Slf4j
@RestController
@RequestMapping("/runtime")
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    private final ActorSystem actorSystem;
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpVilleActorFactory actorFactory) {
        super(actorSystem, actorFactory, "ville-service");
        this.actorSystem = actorSystem;
    }
    
    /**
     * Override createActor to automatically create 3 sensor actors when a VilleActor is created
     */
    @Override
    @PostMapping("/create-actor")
    public ResponseEntity<ActorCreatedResponse> createActor(@RequestBody CreateActorCommand command) {
        log.info("[ville-service] Creating actor: type={}, id={}", command.getActorType(), command.getActorId());
        
        try {
            // Ensure params is initialized
            if (command.getParams() == null) {
                command.setParams(new HashMap<>());
            }
            
            // Always set actorId in params
            command.getParams().put("actorId", command.getActorId());
            
            // Extract climate config from command params (if provided)
            Object climateConfigObj = command.getParams().get("climateConfig");
            
            ActorRef villeRef = actorSystem.spawn(command.getActorType(), command.getParams());
            
            // If this is a VilleActor, create the 3 sensors
            if ("VilleActor".equals(command.getActorType())) {
                createSensorsForCity(command.getActorId(), climateConfigObj);
            }
            
            log.info("[ville-service] Actor created successfully: {}", command.getActorId());
            return ResponseEntity.ok(
                    ActorCreatedResponse.success(
                            command.getActorId(),
                            command.getActorType(),
                            "ville-service"
                    )
            );
        } catch (Exception e) {
            log.error("[ville-service] Failed to create actor: {}", command.getActorId(), e);
            return ResponseEntity.ok(
                    ActorCreatedResponse.failure(
                            command.getActorId(),
                            e.getMessage()
                    )
            );
        }
    }
    
    /**
     * Create 3 sensor actors for a city
     */
    private void createSensorsForCity(String villeId, Object climateConfig) {
        String[] sensorTypes = {"TEMPERATURE", "HUMIDITY", "PRESSURE"};
        
        for (String sensorType : sensorTypes) {
            try {
                String sensorActorId = sensorType.toLowerCase() + "-" + villeId;
                
                Map<String, Object> sensorParams = new HashMap<>();
                sensorParams.put("type", sensorType);
                sensorParams.put("villeId", villeId);
                sensorParams.put("actorId", sensorActorId);
                
                // Pass the climate configuration to the sensor if provided
                if (climateConfig != null) {
                    sensorParams.put("climateConfig", climateConfig);
                    log.debug("[ville-service] Passing climateConfig to sensor {}", sensorActorId);
                }
                
                ActorRef sensorRef = actorSystem.spawn("CapteurActor", sensorParams);
                
                log.info("[ville-service] Created sensor: {} for city: {}", sensorActorId, villeId);
            } catch (Exception e) {
                log.error("[ville-service] Failed to create sensor {} for city {}", sensorType, villeId, e);
            }
        }
    }
}
