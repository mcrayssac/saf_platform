package com.acme.saf.actor.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Simple implementation of the Message interface.
 * This is a basic message implementation that can be used for common messaging patterns.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
public class SimpleMessage implements Message {
    
    private static final long serialVersionUID = 1L;
    
    private final String messageId;
    private final Instant timestamp;
    private final String correlationId;
    private final Object payload;
    
    public SimpleMessage(Object payload) {
        this(UUID.randomUUID().toString(), Instant.now(), null, payload);
    }
    
    public SimpleMessage(Object payload, String correlationId) {
        this(UUID.randomUUID().toString(), Instant.now(), correlationId, payload);
    }
    
    @JsonCreator
    public SimpleMessage(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("payload") Object payload) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.payload = payload;
    }
    
    @Override
    public String getMessageId() {
        return messageId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Object getPayload() {
        return payload;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleMessage that = (SimpleMessage) o;
        return Objects.equals(messageId, that.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
    
    @Override
    public String toString() {
        return String.format("SimpleMessage[id=%s, correlationId=%s, timestamp=%s, payload=%s]",
                messageId, correlationId, timestamp, payload);
    }
}
