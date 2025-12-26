package com.acme.saf.saf_runtime.messaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Message de transport générique pour la communication inter-pods via un broker (Kafka/RabbitMQ).
 * 
 * Cette classe encapsule un message applicatif et ses métadonnées pour la transmission
 * via un broker de messages, en assurant la compatibilité avec différents types de messages.
 */
public class BrokerMessage {
    
    // Identifiant unique du message
    private String messageId;
    
    // Type du message (FQCN de la classe du message applicatif)
    private String messageType;
    
    // Payload sérialisé (JSON ou autre format)
    private String payload;
    
    // Métadonnées
    private Map<String, String> headers;
    
    // ID de l'acteur source
    private String sourcePodId;
    
    // Topic/Queue de destination
    private String destinationTopic;
    
    // Timestamp de création
    private long timestamp;
    
    // Version du protocol
    private String protocolVersion = "1.0";
    
    // Stratégie de traitement en cas d'erreur
    private String errorStrategy = "RETRY"; // RETRY, IGNORE, DLQ
    
    // Nombre de tentatives
    private int retryCount = 3;
    
    public BrokerMessage() {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.headers = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public BrokerMessage(String messageType, String payload, String destinationTopic) {
        this();
        this.messageType = messageType;
        this.payload = payload;
        this.destinationTopic = destinationTopic;
    }
    
    // Getters et Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }
    
    public String getHeader(String key) {
        return this.headers.get(key);
    }
    
    public String getSourcePodId() {
        return sourcePodId;
    }
    
    public void setSourcePodId(String sourcePodId) {
        this.sourcePodId = sourcePodId;
    }
    
    public String getDestinationTopic() {
        return destinationTopic;
    }
    
    public void setDestinationTopic(String destinationTopic) {
        this.destinationTopic = destinationTopic;
    }
    
    public String getTopic() {
        return destinationTopic;
    }
    
    public void setTopic(String topic) {
        this.destinationTopic = topic;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public String getErrorStrategy() {
        return errorStrategy;
    }
    
    public void setErrorStrategy(String errorStrategy) {
        this.errorStrategy = errorStrategy;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    @Override
    public String toString() {
        return "BrokerMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", destinationTopic='" + destinationTopic + '\'' +
                ", sourcePodId='" + sourcePodId + '\'' +
                ", timestamp=" + timestamp +
                ", protocolVersion='" + protocolVersion + '\'' +
                ", errorStrategy='" + errorStrategy + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
