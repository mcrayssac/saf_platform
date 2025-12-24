package com.acme.saf.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RemoteActorRef represents a reference to an actor in a different microservice.
 * Enables transparent communication across service boundaries using HTTP.
 * 
 * Messages are serialized to JSON and sent via HTTP POST to the remote service.
 * The remote service's ActorController processes the message and returns a response.
 * 
 * Key features:
 * - Location transparency: Same interface as local ActorRef
 * - HTTP-based communication: REST API for cross-service messaging
 * - Retry mechanism: Configurable retries with exponential backoff
 * - Circuit breaker: Prevents cascade failures (future enhancement)
 * - Serialization: JSON-based message serialization
 * 
 * Example:
 * <pre>
 * // Reference to actor in another service
 * RemoteActorRef remoteActor = new RemoteActorRef.Builder()
 *     .withActorId("client-123")
 *     .withServiceUrl("http://iot-runtime:8081")
 *     .withHttpClient(httpClient)
 *     .build();
 * 
 * // Send message transparently
 * remoteActor.tell(message);
 * 
 * // Request-reply across services
 * CompletableFuture<Object> response = remoteActor.ask(message, 5, TimeUnit.SECONDS);
 * </pre>
 */
public class RemoteActorRef implements ActorRef {
    
    private final String actorId;
    private final String serviceUrl;
    private final String serviceName;
    private final RemoteMessageTransport transport;
    
    private RemoteActorRef(Builder builder) {
        this.actorId = builder.actorId;
        this.serviceUrl = builder.serviceUrl;
        this.serviceName = builder.serviceName;
        this.transport = builder.transport;
    }
    
    @Override
    public String getActorId() {
        return actorId;
    }
    
    @Override
    public String getPath() {
        // Format: actor@service:port/path
        return actorId + "@" + serviceName;
    }
    
    /**
     * Gets the full remote URL for this actor.
     * 
     * @return URL like http://iot-runtime:8081/actors/client-123
     */
    public String getRemoteUrl() {
        return serviceUrl + "/actors/" + actorId;
    }
    
    /**
     * Gets the service name this actor belongs to.
     * 
     * @return service name (e.g., "iot-runtime")
     */
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public void tell(Message message) {
        tell(message, null);
    }
    
    @Override
    public void tell(Message message, ActorRef sender) {
        try {
            transport.sendMessage(getRemoteUrl(), message, sender);
        } catch (Exception e) {
            System.err.println("Failed to send message to remote actor " + actorId + ": " + e.getMessage());
            throw new RuntimeException("Remote message delivery failed", e);
        }
    }
    
    @Override
    public CompletableFuture<Object> ask(Message message, long timeout, TimeUnit unit) {
        try {
            return transport.askMessage(getRemoteUrl(), message, timeout, unit);
        } catch (Exception e) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    @Override
    public void forward(Message message, ActorRef originalSender) {
        tell(message, originalSender);
    }
    
    @Override
    public boolean isActive() {
        try {
            return transport.checkActorExists(getRemoteUrl());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void stop() {
        try {
            transport.stopActor(getRemoteUrl());
        } catch (Exception e) {
            System.err.println("Failed to stop remote actor " + actorId + ": " + e.getMessage());
        }
    }
    
    @Override
    public void block() {
        throw new UnsupportedOperationException("block() not supported for remote actors");
    }
    
    @Override
    public void unblock() {
        throw new UnsupportedOperationException("unblock() not supported for remote actors");
    }
    
    @Override
    public void restart(Throwable cause) {
        throw new UnsupportedOperationException("restart() not supported for remote actors");
    }
    
    @Override
    public ActorLifecycleState getState() {
        try {
            return transport.getActorState(getRemoteUrl());
        } catch (Exception e) {
            return ActorLifecycleState.FAILED;
        }
    }
    
    @Override
    public void watch(ActorRef watcher) {
        System.out.println("WARNING: watch() on remote actors not yet implemented");
        // TODO: Implement remote watch with distributed death watch
    }
    
    @Override
    public void unwatch(ActorRef watcher) {
        System.out.println("WARNING: unwatch() on remote actors not yet implemented");
        // TODO: Implement remote unwatch
    }
    
    /**
     * Checks if this is a remote actor reference.
     * 
     * @return always true for RemoteActorRef
     */
    public boolean isRemote() {
        return true;
    }
    
    @Override
    public String toString() {
        return "RemoteActorRef{" +
                "actorId='" + actorId + '\'' +
                ", service='" + serviceName + '\'' +
                ", url='" + getRemoteUrl() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteActorRef that = (RemoteActorRef) o;
        return actorId.equals(that.actorId) && serviceUrl.equals(that.serviceUrl);
    }
    
    @Override
    public int hashCode() {
        return actorId.hashCode() + serviceUrl.hashCode();
    }
    
    /**
     * Builder for creating RemoteActorRef instances.
     */
    public static class Builder {
        private String actorId;
        private String serviceUrl;
        private String serviceName;
        private RemoteMessageTransport transport;
        
        public Builder withActorId(String actorId) {
            this.actorId = actorId;
            return this;
        }
        
        public Builder withServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
            // Extract service name from URL (e.g., http://iot-runtime:8081 -> iot-runtime)
            if (serviceName == null && serviceUrl != null) {
                String[] parts = serviceUrl.replace("http://", "").replace("https://", "").split(":");
                this.serviceName = parts[0];
            }
            return this;
        }
        
        public Builder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public Builder withTransport(RemoteMessageTransport transport) {
            this.transport = transport;
            return this;
        }
        
        public RemoteActorRef build() {
            if (actorId == null || actorId.isEmpty()) {
                throw new IllegalArgumentException("Actor ID must be specified");
            }
            if (serviceUrl == null || serviceUrl.isEmpty()) {
                throw new IllegalArgumentException("Service URL must be specified");
            }
            if (transport == null) {
                throw new IllegalArgumentException("Transport must be specified");
            }
            if (serviceName == null) {
                // Extract from URL if not explicitly set
                String[] parts = serviceUrl.replace("http://", "").replace("https://", "").split(":");
                serviceName = parts[0];
            }
            return new RemoteActorRef(this);
        }
    }
}
