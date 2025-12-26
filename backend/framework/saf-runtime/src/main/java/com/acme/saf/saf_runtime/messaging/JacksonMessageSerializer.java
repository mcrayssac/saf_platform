package com.acme.saf.saf_runtime.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implémentation de MessageSerializer utilisant Jackson pour la sérialisation JSON.
 * 
 * Cette classe gère la conversion entre les messages applicatifs et les BrokerMessages,
 * en utilisant Jackson comme sérialiseur JSON pour assurer la compatibilité maximale.
 */
public class JacksonMessageSerializer implements MessageSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(JacksonMessageSerializer.class);
    
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Class<?>> messageTypeRegistry;
    
    public JacksonMessageSerializer() {
        this.objectMapper = createConfiguredObjectMapper();
        this.messageTypeRegistry = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates and configures an ObjectMapper with Java 8 time support.
     */
    private static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    public JacksonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.messageTypeRegistry = new ConcurrentHashMap<>();
    }
    
    @Override
    public BrokerMessage serialize(Object message, String destinationTopic) 
            throws Exception {
        
        if (message == null) {
            throw new Exception("Cannot serialize null message");
        }
        
        try {
            String messageType = message.getClass().getName();
            String messagePayload = objectMapper.writeValueAsString(message);
            
            // Wrap payload with messageType for consumer deserialization
            MessageEnvelope envelope = new MessageEnvelope(messageType, messagePayload);
            String envelopedPayload = objectMapper.writeValueAsString(envelope);
            
            BrokerMessage brokerMessage = new BrokerMessage(messageType, envelopedPayload, destinationTopic);
            brokerMessage.addHeader("className", message.getClass().getSimpleName());
            brokerMessage.addHeader("package", message.getClass().getPackageName());
            
            logger.debug("Message serialized: {} -> {}", messageType, brokerMessage.getMessageId());
            return brokerMessage;
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to serialize message of type %s", 
                    message.getClass().getName());
            logger.error(errorMsg, e);
            throw e;
        }
    }
    
    @Override
    public <T> T deserialize(BrokerMessage brokerMessage, Class<T> targetClass) 
            throws Exception {
        
        if (brokerMessage == null || brokerMessage.getPayload() == null) {
            throw new Exception("Cannot deserialize null brokerMessage or payload");
        }
        
        try {
            String payload = brokerMessage.getPayload();
            
            // Try to unwrap from MessageEnvelope first (new format)
            try {
                MessageEnvelope envelope = objectMapper.readValue(payload, MessageEnvelope.class);
                if (envelope != null && envelope.getPayload() != null && envelope.getMessageType() != null) {
                    // This is wrapped - use the unwrapped payload
                    payload = envelope.getPayload();
                    logger.debug("Unwrapped message envelope, using inner payload");
                }
            } catch (Exception e) {
                // Not an envelope, use payload as-is (legacy format or direct JSON)
                logger.debug("Payload is not wrapped in MessageEnvelope, using as-is");
            }
            
            T result = objectMapper.readValue(payload, targetClass);
            
            logger.debug("Message deserialized: {} -> {}", 
                    brokerMessage.getMessageType(), targetClass.getName());
            return result;
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to deserialize message to type %s", 
                    targetClass.getName());
            logger.error(errorMsg, e);
            throw e;
        }
    }
    
    @Override
    public void registerMessageType(String messageType, Class<?> messageClass) {
        messageTypeRegistry.put(messageType, messageClass);
        logger.debug("Registered message type: {} -> {}", messageType, messageClass.getName());
    }
    
    @Override
    public Class<?> getMessageClass(String messageType) {
        return messageTypeRegistry.get(messageType);
    }
    
    /**
     * Retourne le registre des types de messages (utile pour les diagnostics).
     */
    public ConcurrentMap<String, Class<?>> getMessageTypeRegistry() {
        return messageTypeRegistry;
    }
}
