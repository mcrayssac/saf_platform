package com.acme.saf.actor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Router that broadcasts messages to all routees.
 * Each message is sent to every routee in the pool.
 * 
 * This strategy is useful for:
 * - Publish-subscribe patterns
 * - Broadcasting notifications or updates
 * - Ensuring all actors receive the same message
 * 
 * Thread-safe: Synchronized methods for routee management.
 * 
 * Example:
 * <pre>
 * List&lt;ActorRef&gt; subscribers = Arrays.asList(sub1, sub2, sub3);
 * Router router = new BroadcastRouter(subscribers);
 * 
 * // All subscribers receive the message
 * router.route(notification, sender);
 * </pre>
 */
public class BroadcastRouter implements Router {
    
    private final List<ActorRef> routees;
    
    /**
     * Creates a BroadcastRouter with the specified routees.
     * 
     * @param routees the initial list of routees
     * @throws IllegalArgumentException if routees is null or empty
     */
    public BroadcastRouter(List<ActorRef> routees) {
        if (routees == null || routees.isEmpty()) {
            throw new IllegalArgumentException("Routees list cannot be null or empty");
        }
        this.routees = new ArrayList<>(routees);
    }
    
    /**
     * Creates a BroadcastRouter with a single routee.
     * 
     * @param routee the initial routee
     */
    public BroadcastRouter(ActorRef routee) {
        this(Collections.singletonList(routee));
    }
    
    @Override
    public void route(Message message, ActorRef sender) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available for routing");
        }
        
        // Send message to all routees
        List<ActorRef> currentRoutees = getRoutees();
        for (ActorRef routee : currentRoutees) {
            try {
                if (sender != null) {
                    routee.tell(message, sender);
                } else {
                    routee.tell(message);
                }
            } catch (Exception e) {
                // Log error but continue broadcasting to other routees
                System.err.println("Error broadcasting to routee " + routee.getActorId() + ": " + e.getMessage());
            }
        }
    }
    
    @Override
    public synchronized List<ActorRef> getRoutees() {
        return Collections.unmodifiableList(new ArrayList<>(routees));
    }
    
    @Override
    public synchronized void addRoutee(ActorRef routee) {
        if (routee == null) {
            throw new IllegalArgumentException("Routee cannot be null");
        }
        if (!routees.contains(routee)) {
            routees.add(routee);
        }
    }
    
    @Override
    public synchronized void removeRoutee(ActorRef routee) {
        routees.remove(routee);
    }
    
    @Override
    public String toString() {
        return "BroadcastRouter{" +
                "routees=" + routees.size() +
                '}';
    }
}
