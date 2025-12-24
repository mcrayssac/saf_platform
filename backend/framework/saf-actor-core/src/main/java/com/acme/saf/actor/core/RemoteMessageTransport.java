package com.acme.saf.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RemoteMessageTransport defines the contract for sending messages to remote actors.
 * Implementations handle the actual HTTP communication, serialization, and error handling.
 * 
 * This abstraction allows different transport mechanisms (HTTP, gRPC, WebSocket, etc.)
 * while keeping RemoteActorRef independent of the underlying protocol.
 * 
 * Responsibilities:
 * - Message serialization/deserialization
 * - HTTP communication with remote services
 * - Retry logic and error handling
 * - Circuit breaker integration (optional)
 * - Connection pooling and timeout management
 * 
 * Example implementation using Spring WebClient:
 * <pre>
 * public class HttpRemoteTransport implements RemoteMessageTransport {
 *     private final WebClient webClient;
 *     
 *     public void sendMessage(String url, Message message, ActorRef sender) {
 *         webClient.post()
 *             .uri(url + "/messages")
 *             .bodyValue(serialize(message))
 *             .retrieve()
 *             .bodyToMono(Void.class)
 *             .block();
 *     }
 * }
 * </pre>
 */
public interface RemoteMessageTransport {
    
    /**
     * Sends a message to a remote actor (fire-and-forget).
     * 
     * @param actorUrl the full URL to the remote actor (e.g., http://service:8081/actors/id)
     * @param message the message to send
     * @param sender the sender actor reference (can be null)
     * @throws Exception if message delivery fails
     */
    void sendMessage(String actorUrl, Message message, ActorRef sender) throws Exception;
    
    /**
     * Sends a message to a remote actor and waits for a response (request-reply).
     * 
     * @param actorUrl the full URL to the remote actor
     * @param message the message to send
     * @param timeout the maximum time to wait for a response
     * @param unit the time unit of the timeout
     * @return a CompletableFuture containing the response
     * @throws Exception if message delivery fails
     */
    CompletableFuture<Object> askMessage(String actorUrl, Message message, long timeout, TimeUnit unit) throws Exception;
    
    /**
     * Checks if a remote actor exists and is active.
     * 
     * @param actorUrl the full URL to the remote actor
     * @return true if the actor exists and is active
     * @throws Exception if the check fails
     */
    boolean checkActorExists(String actorUrl) throws Exception;
    
    /**
     * Stops a remote actor.
     * 
     * @param actorUrl the full URL to the remote actor
     * @throws Exception if the stop operation fails
     */
    void stopActor(String actorUrl) throws Exception;
    
    /**
     * Gets the lifecycle state of a remote actor.
     * 
     * @param actorUrl the full URL to the remote actor
     * @return the actor's lifecycle state
     * @throws Exception if the state cannot be retrieved
     */
    ActorLifecycleState getActorState(String actorUrl) throws Exception;
}
