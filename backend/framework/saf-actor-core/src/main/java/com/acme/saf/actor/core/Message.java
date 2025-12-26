package com.acme.saf.actor.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all messages exchanged between actors.
 * Messages are immutable and serializable to support distributed actor systems.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class"
)
@JsonSubTypes({
    // Subtype list will be auto-discovered at runtime
})
public interface Message extends Serializable {
    
    /**
     * Unique identifier for this message instance.
     * @return the message ID
     */
    default String getMessageId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Timestamp when the message was created.
     * @return the creation timestamp
     */
    default Instant getTimestamp() {
        return Instant.now();
    }
    
    /**
     * Optional correlation ID for tracking request-response pairs.
     * @return the correlation ID, or null if not applicable
     */
    default String getCorrelationId() {
        return null;
    }
    
    /**
     * The actual payload/content of the message.
     * Implementations should override this to provide specific message data.
     * @return the message payload
     */
    Object getPayload();
}
