package com.acme.saf.saf_runtime.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implémentation du consommateur de messages avec Kafka.
 * 
 * Gère la réception, désérialisation et dispatch des messages vers les listeners enregistrés.
 */
public class DefaultMessageConsumer implements MessageConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageConsumer.class);
    
    private final MessageSerializer serializer;
    private KafkaConsumer<String, String> kafkaConsumer;
    private volatile boolean connected = false;
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private Thread consumerThread;
    
    // Structure: messageType -> (listener, errorHandler)
    private final ConcurrentMap<String, ListenerEntry<?>> listeners = new ConcurrentHashMap<>();
    
    // Shared ObjectMapper with Java 8 time support
    private static final ObjectMapper sharedObjectMapper = createConfiguredObjectMapper();
    
    /**
     * Creates and configures an ObjectMapper with Java 8 time support.
     */
    private static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    public DefaultMessageConsumer(MessageSerializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("MessageSerializer cannot be null");
        }
        this.serializer = serializer;
        initKafkaConsumer();
    }
    
    /**
     * Initialise le client Kafka consumer avec les paramètres par défaut.
     */
    private void initKafkaConsumer() {
        try {
            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
            kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaProps.put("group.id", "saf-framework-" + System.nanoTime());
            kafkaProps.put("auto.offset.reset", "earliest");
            kafkaProps.put("enable.auto.commit", "true");
            kafkaProps.put("auto.commit.interval.ms", "1000");
            
            this.kafkaConsumer = new KafkaConsumer<>(kafkaProps);
            this.connected = true;
            logger.info("Kafka consumer initialized with brokers: {}", kafkaProps.get("bootstrap.servers"));
        } catch (Exception e) {
            logger.error("Failed to initialize Kafka consumer", e);
            this.connected = false;
        }
    }
    
    @Override
    public <T> void subscribe(String messageType, Class<T> messageClass, Consumer<T> listener) {
        subscribe(messageType, messageClass, listener, null);
    }
    
    @Override
    public <T> void subscribe(String messageType, Class<T> messageClass, Consumer<T> listener,
            Consumer<Exception> errorHandler) {
        
        if (messageType == null || messageType.isEmpty()) {
            throw new IllegalArgumentException("MessageType cannot be null or empty");
        }
        if (messageClass == null) {
            throw new IllegalArgumentException("MessageClass cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        // Enregistrer le listener
        ListenerEntry<T> entry = new ListenerEntry<>(messageClass, listener, errorHandler);
        listeners.put(messageType, entry);
        
        // Enregistrer le type de message dans le sérialiseur
        serializer.registerMessageType(messageType, messageClass);
        
        logger.debug("Subscribed to message type: {} with class: {}", messageType, messageClass.getName());
    }
    
    @Override
    public void listen(String topic) throws Exception {
        if (topic == null || topic.isEmpty()) {
            throw new Exception("Topic cannot be null or empty");
        }
        if (!isConnected()) {
            throw new Exception("Consumer is not connected to Kafka broker");
        }
        
        if (listening.getAndSet(true)) {
            logger.warn("Consumer is already listening");
            return;
        }
        
        // S'abonner au topic
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        logger.info("Subscribed to Kafka topic: {}", topic);
        
        // Démarrer un thread pour consommer les messages
        consumerThread = new Thread(() -> {
            try {
                while (listening.get() && connected) {
                    try {
                        var records = kafkaConsumer.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, String> record : records) {
                            try {
                                // Créer un BrokerMessage à partir du record Kafka
                                BrokerMessage brokerMessage = new BrokerMessage();
                                brokerMessage.setMessageId(record.key());
                                brokerMessage.setPayload(record.value());
                                brokerMessage.setTopic(record.topic());
                                brokerMessage.setTimestamp(record.timestamp());
                                
                                // Gérer le message
                                handleIncomingMessage(brokerMessage);
                            } catch (Exception e) {
                                logger.error("Error processing message from Kafka", e);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error polling from Kafka", e);
                    }
                }
            } finally {
                listening.set(false);
                logger.info("Consumer thread stopped for topic: {}", topic);
            }
        });
        
        consumerThread.setName("kafka-consumer-" + topic);
        consumerThread.setDaemon(false);
        consumerThread.start();
        
        logger.debug("Listening to Kafka topic: {}", topic);
    }
    
    @Override
    public void stopListening(String topic) throws Exception {
        if (topic == null || topic.isEmpty()) {
            throw new Exception("Topic cannot be null or empty");
        }
        
        listening.set(false);
        if (consumerThread != null) {
            consumerThread.join(5000);
        }
        
        logger.debug("Stopped listening to topic: {}", topic);
    }
    
    @Override
    public boolean isConnected() {
        return connected && kafkaConsumer != null;
    }
    
    @Override
    public void close() throws Exception {
        listening.set(false);
        if (consumerThread != null && consumerThread.isAlive()) {
            consumerThread.join(5000);
        }
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        this.connected = false;
        logger.info("Consumer closed");
    }
    
    /**
     * Traite un message reçu du broker en le déspatchant au listener approprié.
     */
    public void handleIncomingMessage(BrokerMessage brokerMessage) {
        if (brokerMessage == null) {
            logger.warn("Received null broker message");
            return;
        }
        
        String messageType = brokerMessage.getMessageType();
        
        // If no messageType in BrokerMessage, try multiple strategies
        if (messageType == null || messageType.isEmpty()) {
            // Strategy 1: Try to parse as MessageEnvelope to get the real type (new format)
            try {
                MessageEnvelope envelope = 
                    sharedObjectMapper.readValue(brokerMessage.getPayload(), MessageEnvelope.class);
                if (envelope != null && envelope.getMessageType() != null) {
                    messageType = envelope.getMessageType();
                    brokerMessage.setMessageType(messageType);
                    logger.debug("✓ Extracted messageType from envelope: {}", messageType);
                }
            } catch (Exception e) {
                logger.debug("Payload is not wrapped in MessageEnvelope (legacy format), trying to infer type");
            }
            
            // Strategy 2: Try to infer type from registered listeners by trying to deserialize with each one
            if ((messageType == null || messageType.isEmpty()) && !listeners.isEmpty()) {
                for (java.util.Map.Entry<String, ListenerEntry<?>> entry : listeners.entrySet()) {
                    try {
                        String candidateType = entry.getKey();
                        ListenerEntry<?> listener = entry.getValue();
                        Object deserializedObj = sharedObjectMapper.readValue(brokerMessage.getPayload(), listener.getMessageClass());
                        // If it deserialized successfully, this is likely the right type
                        if (deserializedObj != null) {
                            messageType = candidateType;
                            brokerMessage.setMessageType(messageType);
                            logger.info("✓ Inferred messageType from payload structure: {}", messageType);
                            break;
                        }
                    } catch (Exception e) {
                        // Try next listener
                        logger.debug("Could not deserialize as {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        }
        
        // Check if we have a messageType before trying to look it up
        if (messageType == null || messageType.isEmpty()) {
            logger.warn("Could not determine messageType for message from topic: {} - skipping", brokerMessage.getTopic());
            return;
        }
        
        ListenerEntry<?> entry = listeners.get(messageType);
        
        if (entry == null) {
            logger.debug("No listener registered for message type: {} from topic: {}", 
                    messageType, brokerMessage.getTopic());
            return;
        }
        
        try {
            // Désérialiser le message
            Object message = serializer.deserialize(brokerMessage, entry.getMessageClass());
            
            // Appeler le listener
            ((Consumer<Object>) entry.getListener()).accept(message);
            
            logger.info("✓ Message handled successfully: {} (type: {})", 
                    brokerMessage.getMessageId(), messageType);
            
        } catch (Exception e) {
            logger.error("Error handling message: {}", brokerMessage.getMessageId(), e);
            
            // Appeler le error handler s'il existe
            Consumer<Exception> errorHandler = entry.getErrorHandler();
            if (errorHandler != null) {
                try {
                    errorHandler.accept(e);
                } catch (Exception ehException) {
                    logger.error("Error in error handler", ehException);
                }
            }
        }
    }
    
    /**
     * Classe interne pour stocker les listeners et leurs error handlers.
     */
    private static class ListenerEntry<T> {
        private final Class<T> messageClass;
        private final Consumer<T> listener;
        private final Consumer<Exception> errorHandler;
        
        ListenerEntry(Class<T> messageClass, Consumer<T> listener, Consumer<Exception> errorHandler) {
            this.messageClass = messageClass;
            this.listener = listener;
            this.errorHandler = errorHandler;
        }
        
        Class<T> getMessageClass() {
            return messageClass;
        }
        
        Consumer<T> getListener() {
            return listener;
        }
        
        Consumer<Exception> getErrorHandler() {
            return errorHandler;
        }
    }
}
