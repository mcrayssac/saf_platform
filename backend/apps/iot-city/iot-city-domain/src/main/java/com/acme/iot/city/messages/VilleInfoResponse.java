package com.acme.iot.city.messages;

import com.acme.iot.city.model.VilleInfo;
import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response message containing detailed city information.
 * Sent by VilleActor in response to RequestVilleInfo.
 */
@JsonTypeName("VilleInfoResponse")
public class VilleInfoResponse implements Message {
    private static final long serialVersionUID = 1L;
    
    /**
     * Type hint for Jackson deserialization across Kafka.
     */
    private final String messageType = "com.acme.iot.city.messages.VilleInfoResponse";
    
    private final VilleInfo villeInfo;
    
    @JsonCreator
    public VilleInfoResponse(@JsonProperty("villeInfo") VilleInfo villeInfo) {
        this.villeInfo = villeInfo;
    }
    
    public VilleInfo getVilleInfo() {
        return villeInfo;
    }
    
    @Override
    public Object getPayload() {
        return villeInfo;
    }
    
    @Override
    public String toString() {
        return "VilleInfoResponse{villeInfo=" + villeInfo + "}";
    }
}
