package com.acme.saf.saf_runtime.messaging;

/**
 * Envelope pour wrapper les messages avec métadonnées de type.
 * Permet au consommateur de connaître le type du message sans enregistrement préalable.
 */
public class MessageEnvelope {
    private String messageType;
    private String payload;
    
    public MessageEnvelope() {
    }
    
    public MessageEnvelope(String messageType, String payload) {
        this.messageType = messageType;
        this.payload = payload;
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
}
