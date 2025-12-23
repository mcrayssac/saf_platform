package com.acme.saf.actor.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reference to an actor that allows sending messages without direct access to the actor instance.
 * ActorRef provides location transparency - the caller doesn't need to know where the actor is located.
 * 
 * This is the primary way to interact with actors from outside the actor system.
 */
public interface ActorRef {
    
    /**
     * Gets the unique identifier of the actor.
     * @return the actor ID
     */
    String getActorId();
    
    /**
     * Gets the path of the actor in the actor hierarchy.
     * Format: /user/parent/child
     * @return the actor path
     */
    String getPath();
    
    /**
     * Sends a message to the actor asynchronously (fire-and-forget).
     * This is the "tell" pattern - the sender doesn't wait for a response.
     * 
     * @param message the message to send
     */
    void tell(Message message);
    
    /**
     * Sends a message to the actor asynchronously with a sender reference.
     * The receiver can reply to the sender using the provided reference.
     * 
     * @param message the message to send
     * @param sender the sender's actor reference (can be null)
     */
    void tell(Message message, ActorRef sender);
    
    /**
     * Sends a message to the actor and returns a future for the response (ask pattern).
     * This allows request-response communication with timeout support.
     * 
     * @param message the message to send
     * @param timeout the maximum time to wait for a response
     * @param unit the time unit of the timeout
     * @return a CompletableFuture that will complete with the response
     */
    CompletableFuture<Object> ask(Message message, long timeout, TimeUnit unit);
    
    /**
     * Sends a message to the actor and returns a future for the response with default timeout.
     * 
     * @param message the message to send
     * @return a CompletableFuture that will complete with the response
     */
    default CompletableFuture<Object> ask(Message message) {
        return ask(message, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Forwards a message to this actor, preserving the original sender.
     * Used for message routing and delegation patterns.
     * 
     * @param message the message to forward
     * @param originalSender the original sender of the message
     */
    void forward(Message message, ActorRef originalSender);
    
    /**
     * Checks if the actor is currently active and can receive messages.
     * @return true if the actor is active, false otherwise
     */
    boolean isActive();
    
    /**
     * Stops the actor.
     * The actor will be removed from the system and can no longer process messages.
     */
    void stop();
    
    /**
     * Blocks the actor from processing messages.
     * Messages sent to a blocked actor will be queued.
     * The actor can be unblocked later.
     */
    void block();
    
    /**
     * Unblocks a blocked actor.
     * The actor will resume processing queued messages.
     */
    void unblock();
    
    /**
     * Restarts the actor.
     * Triggers preRestart() and postRestart() lifecycle callbacks.
     * The actor's mailbox is preserved.
     * 
     * @param cause the reason for the restart
     */
    void restart(Throwable cause);
    
    /**
     * Gets the current lifecycle state of the actor.
     * 
     * @return the actor's current state
     */
    ActorLifecycleState getState();
    
    /**
     * Starts watching this actor for termination.
     * When this actor terminates, the watcher will receive a Terminated message.
     * This is the DeathWatch pattern for monitoring actor lifecycle.
     * 
     * @param watcher the actor that will watch this actor
     */
    void watch(ActorRef watcher);
    
    /**
     * Stops watching this actor for termination.
     * The watcher will no longer receive Terminated messages when this actor stops.
     * 
     * @param watcher the actor that will stop watching
     */
    void unwatch(ActorRef watcher);
}
