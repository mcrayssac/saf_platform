package com.acme.saf.saf_control.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a distributed actor.
 * Received from clients (e.g., frontend).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateActorRequest {
    
    /**
     * ID of the service where the actor should be created
     */
    private String serviceId;
    
    /**
     * Type of actor to create (e.g., "ClientActor", "VilleActor")
     */
    private String actorType;
    
    /**
     * Initialization parameters for the actor
     */
    private Map<String, Object> params;
}
