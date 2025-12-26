package com.acme.saf.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RemoteActorRefProxy - Proxy for remote actors accessible via RemoteMessageTransport.
 * 
 * This class allows actors to send messages to actors in other microservices
 * by delegating the message delivery to a RemoteMessageTransport implementation.
 * 
 * Note: This is a simplified proxy that only supports the tell() pattern.
 * Other ActorRef methods throw UnsupportedOperationException for remote actors.
 */
public class RemoteActorRefProxy implements ActorRef {
    
    private final String actorId;
    private final RemoteMessageTransport transport;
    private final ActorRef sender;
    
    public RemoteActorRefProxy(String actorId, RemoteMessageTransport transport, ActorRef sender) {
        this.actorId = actorId;
        this.transport = transport;
        this.sender = sender;
    }
    
    @Override
    public String getActorId() {
        return actorId;
    }
    
    @Override
    public String getPath() {
        return "/remote/" + actorId;
    }
    
    @Override
    public void tell(Message message) {
        tell(message, sender);
    }
    
    @Override
    public void tell(Message message, ActorRef sender) {
        // Use the configured transport to send the message remotely
        if (transport != null) {
            try {
                // We need to resolve the full actor URL - for now use actorId as URL
                // In a real implementation, this would use service discovery
                transport.sendMessage(actorId, message, sender != null ? sender : this.sender);
            } catch (Exception e) {
                System.err.println("Failed to send remote message to " + actorId + ": " + e.getMessage());
            }
        } else {
            System.err.println("No remote transport configured for actor: " + actorId);
        }
    }
    
    @Override
    public CompletableFuture<Object> ask(Message message, long timeout, TimeUnit unit) {
        if (transport != null) {
            try {
                return transport.askMessage(actorId, message, timeout, unit);
            } catch (Exception e) {
                CompletableFuture<Object> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("No remote transport configured"));
        return future;
    }
    
    @Override
    public void forward(Message message, ActorRef originalSender) {
        tell(message, originalSender);
    }
    
    @Override
    public boolean isActive() {
        if (transport != null) {
            try {
                return transport.checkActorExists(actorId);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    @Override
    public void stop() {
        if (transport != null) {
            try {
                transport.stopActor(actorId);
            } catch (Exception e) {
                System.err.println("Failed to stop remote actor " + actorId + ": " + e.getMessage());
            }
        }
    }
    
    @Override
    public void block() {
        throw new UnsupportedOperationException("Cannot block remote actor directly");
    }
    
    @Override
    public void unblock() {
        throw new UnsupportedOperationException("Cannot unblock remote actor directly");
    }
    
    @Override
    public void restart(Throwable cause) {
        throw new UnsupportedOperationException("Cannot restart remote actor directly");
    }
    
    @Override
    public ActorLifecycleState getState() {
        if (transport != null) {
            try {
                return transport.getActorState(actorId);
            } catch (Exception e) {
                return ActorLifecycleState.STOPPED;
            }
        }
        return ActorLifecycleState.STOPPED;
    }
    
    @Override
    public void watch(ActorRef watcher) {
        // Remote watching not supported
        System.err.println("Warning: Remote actor watching not supported");
    }
    
    @Override
    public void unwatch(ActorRef watcher) {
        // Remote watching not supported
    }
}
