package com.acme.saf.saf_runtime.messaging;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Implémentation du producteur de messages avec Kafka.
 * 
 * Gère la sérialisation et l'envoi des messages vers Kafka.
 */
public class DefaultMessageProducer implements MessageProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageProducer.class);
    
    private final MessageSerializer serializer;
    private KafkaProducer<String, String> kafkaProducer;
    private volatile boolean connected = false;
    
    public DefaultMessageProducer(MessageSerializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("MessageSerializer cannot be null");
        }
        this.serializer = serializer;
        initKafkaProducer();
    }
    
    /**
     * Initialise le client Kafka producer avec les paramètres par défaut.
     */
    private void initKafkaProducer() {
        try {
            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
            kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put("acks", "1"); // Leader ack suffisant
            kafkaProps.put("retries", "3");
            
            this.kafkaProducer = new KafkaProducer<>(kafkaProps);
            this.connected = true;
            logger.info("Kafka producer initialized with brokers: {}", kafkaProps.get("bootstrap.servers"));
        } catch (Exception e) {
            logger.error("Failed to initialize Kafka producer", e);
            this.connected = false;
        }
    }
    
    @Override
    public void send(Object message, String destinationTopic) throws Exception {
        send(message, destinationTopic, message.getClass().getName());
    }
    
    @Override
    public void send(Object message, String destinationTopic, String messageType) throws Exception {
        if (message == null) {
            throw new Exception("Message cannot be null");
        }
        if (destinationTopic == null || destinationTopic.isEmpty()) {
            throw new Exception("Destination topic cannot be null or empty");
        }
        if (!isConnected()) {
            throw new Exception("Producer is not connected to Kafka broker");
        }
        
        try {
            // Sérialiser le message
            BrokerMessage brokerMessage = serializer.serialize(message, destinationTopic);
            brokerMessage.setMessageType(messageType);
            
            // Envoyer vers Kafka
            String payload = brokerMessage.getPayload();
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    destinationTopic,
                    brokerMessage.getMessageId(),
                    payload
            );
            
            kafkaProducer.send(record).get(); // Bloquant
            
            logger.debug("Message sent successfully: {} -> {}", 
                    brokerMessage.getMessageId(), destinationTopic);
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send message to topic: %s", destinationTopic);
            logger.error(errorMsg, e);
            throw e;
        }
    }
    
    @Override
    public void sendAsync(Object message, String destinationTopic) {
        if (!isConnected()) {
            logger.warn("Producer not connected, cannot send async message");
            return;
        }
        
        try {
            // Sérialiser le message
            BrokerMessage brokerMessage = serializer.serialize(message, destinationTopic);
            
            // Envoyer vers Kafka (asynchrone)
            String payload = brokerMessage.getPayload();
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    destinationTopic,
                    brokerMessage.getMessageId(),
                    payload
            );
            
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send async message to topic: {}", destinationTopic, exception);
                } else {
                    logger.debug("Async message sent successfully: {} -> {}", 
                            brokerMessage.getMessageId(), destinationTopic);
                }
            });
            
        } catch (Exception e) {
            logger.error("Failed to send async message to topic: {}", destinationTopic, e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected && kafkaProducer != null;
    }
    
    @Override
    public void close() throws Exception {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        this.connected = false;
        logger.info("Producer closed");
    }
}
