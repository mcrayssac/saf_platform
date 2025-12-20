package com.acme.saf.actor.core;

import java.util.concurrent.TimeUnit;


public interface Dispatcher {
    
    /**
     * Schedules an actor for execution to process pending messages.
  	 */
    void dispatch(ActorRef actorRef, Mailbox mailbox, Actor actor, SupervisionStrategy supervisionStrategy);
    
    /**
     * Gracefully shuts down the dispatcher.
     */
    void shutdown();
    
    /**
     * Immediately shuts down the dispatcher without waiting.
     */
    void shutdownNow();
    
    /**
     * Checks if the dispatcher has been shut down.
     */
    boolean isShutdown();
    
    /**
     * Checks if all actors have finished executing after shutdown.
     */
    boolean isTerminated();
    
    /**
     * Waits for the dispatcher to terminate after a shutdown.
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}