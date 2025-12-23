package com.acme.saf.actor.core.protocol;

import com.acme.saf.actor.core.Message;

/**
 * Command to send a message to an actor (fire-and-forget).
 * Sent from saf-control to microservices.
 */
public class TellActorCommand {
    
    private String targetActorId;
    private String senderActorId;
    private Message message;
    
    public TellActorCommand() {
    }
    
    public TellActorCommand(String targetActorId, String senderActorId, Message message) {
        this.targetActorId = targetActorId;
        this.senderActorId = senderActorId;
        this.message = message;
    }
    
    public String getTargetActorId() {
        return targetActorId;
    }
    
    public void setTargetActorId(String targetActorId) {
        this.targetActorId = targetActorId;
    }
    
    public String getSenderActorId() {
        return senderActorId;
    }
    
    public void setSenderActorId(String senderActorId) {
        this.senderActorId = senderActorId;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public void setMessage(Message message) {
        this.message = message;
    }
}
