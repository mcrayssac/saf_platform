package com.acme.saf.actor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Router that distributes messages in round-robin fashion.
 * Each message is sent to the next routee in sequence, cycling back to the first
 * after reaching the last.
 * 
 * This strategy ensures even distribution of messages across all routees,
 * making it ideal for load balancing when all routees have similar processing capacity.
 * 
 * Thread-safe: Uses atomic operations for index management.
 * 
 * Example:
 * <pre>
 * List&lt;ActorRef&gt; workers = Arrays.asList(worker1, worker2, worker3);
 * Router router = new RoundRobinRouter(workers);
 * 
 * // Messages distributed: worker1, worker2, worker3, worker1, worker2, ...
 * for (Message msg : messages) {
 *     router.route(msg, sender);
 * }
 * </pre>
 */
public class RoundRobinRouter implements Router {
    
    private final List<ActorRef> routees;
    private final AtomicInteger currentIndex;
    
    /**
     * Creates a RoundRobinRouter with the specified routees.
     * 
     * @param routees the initial list of routees
     * @throws IllegalArgumentException if routees is null or empty
     */
    public RoundRobinRouter(List<ActorRef> routees) {
        if (routees == null || routees.isEmpty()) {
            throw new IllegalArgumentException("Routees list cannot be null or empty");
        }
        this.routees = new ArrayList<>(routees);
        this.currentIndex = new AtomicInteger(0);
    }
    
    /**
     * Creates a RoundRobinRouter with a single routee.
     * 
     * @param routee the initial routee
     */
    public RoundRobinRouter(ActorRef routee) {
        this(Collections.singletonList(routee));
    }
    
    @Override
    public void route(Message message, ActorRef sender) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available for routing");
        }
        
        // Get next routee in round-robin fashion
        int index = getNextIndex();
        ActorRef routee = routees.get(index);
        
        // Forward message to selected routee
        if (sender != null) {
            routee.tell(message, sender);
        } else {
            routee.tell(message);
        }
    }
    
    /**
     * Gets the next index in round-robin fashion, wrapping around when necessary.
     * Thread-safe using atomic operations.
     * 
     * @return the next index
     */
    private int getNextIndex() {
        int size = routees.size();
        if (size == 1) {
            return 0;
        }
        
        // Atomically increment and wrap around
        return currentIndex.getAndUpdate(i -> (i + 1) % size);
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
        // Reset index if we removed the current or later routee
        if (currentIndex.get() >= routees.size() && !routees.isEmpty()) {
            currentIndex.set(0);
        }
    }
    
    @Override
    public String toString() {
        return "RoundRobinRouter{" +
                "routees=" + routees.size() +
                ", currentIndex=" + currentIndex.get() +
                '}';
    }
}
