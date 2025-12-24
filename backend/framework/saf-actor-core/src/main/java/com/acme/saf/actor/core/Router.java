package com.acme.saf.actor.core;

import java.util.List;

/**
 * Router that distributes messages across multiple actor instances (routees).
 * Routers enable load balancing and parallel processing by delegating work
 * to a pool of actors.
 * 
 * Common use cases:
 * - Load balancing: Distribute work across worker actors
 * - Parallel processing: Process multiple messages simultaneously
 * - Scaling: Add/remove routees dynamically based on load
 * 
 * Example:
 * <pre>
 * Router router = new RoundRobinRouter(workerActors);
 * router.route(message, sender);
 * </pre>
 */
public interface Router {
    
    /**
     * Routes a message to one of the routees based on the routing strategy.
     * 
     * @param message the message to route
     * @param sender the sender of the message (can be null)
     */
    void route(Message message, ActorRef sender);
    
    /**
     * Gets the list of routees (target actors) managed by this router.
     * 
     * @return immutable list of routees
     */
    List<ActorRef> getRoutees();
    
    /**
     * Adds a new routee to the router.
     * 
     * @param routee the actor to add
     */
    void addRoutee(ActorRef routee);
    
    /**
     * Removes a routee from the router.
     * 
     * @param routee the actor to remove
     */
    void removeRoutee(ActorRef routee);
    
    /**
     * Gets the number of routees currently in the router.
     * 
     * @return number of routees
     */
    default int size() {
        return getRoutees().size();
    }
    
    /**
     * Checks if the router has any routees.
     * 
     * @return true if there are no routees
     */
    default boolean isEmpty() {
        return getRoutees().isEmpty();
    }
}
