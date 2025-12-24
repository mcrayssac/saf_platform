package com.acme.saf.actor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Router that distributes messages randomly across routees.
 * Each message is sent to a randomly selected routee.
 * 
 * This strategy is useful when:
 * - You want to avoid predictable patterns
 * - Routees have varying processing times
 * - You want natural load distribution over time
 * 
 * Thread-safe: Uses ThreadLocalRandom for concurrent access.
 * 
 * Example:
 * <pre>
 * List&lt;ActorRef&gt; workers = Arrays.asList(worker1, worker2, worker3);
 * Router router = new RandomRouter(workers);
 * 
 * // Each message goes to a random worker
 * for (Message msg : messages) {
 *     router.route(msg, sender);
 * }
 * </pre>
 */
public class RandomRouter implements Router {
    
    private final List<ActorRef> routees;
    
    /**
     * Creates a RandomRouter with the specified routees.
     * 
     * @param routees the initial list of routees
     * @throws IllegalArgumentException if routees is null or empty
     */
    public RandomRouter(List<ActorRef> routees) {
        if (routees == null || routees.isEmpty()) {
            throw new IllegalArgumentException("Routees list cannot be null or empty");
        }
        this.routees = new ArrayList<>(routees);
    }
    
    /**
     * Creates a RandomRouter with a single routee.
     * 
     * @param routee the initial routee
     */
    public RandomRouter(ActorRef routee) {
        this(Collections.singletonList(routee));
    }
    
    @Override
    public void route(Message message, ActorRef sender) {
        if (routees.isEmpty()) {
            throw new IllegalStateException("No routees available for routing");
        }
        
        // Get random routee
        ActorRef routee = selectRandomRoutee();
        
        // Forward message to selected routee
        if (sender != null) {
            routee.tell(message, sender);
        } else {
            routee.tell(message);
        }
    }
    
    /**
     * Selects a random routee from the available routees.
     * Thread-safe using ThreadLocalRandom.
     * 
     * @return a randomly selected routee
     */
    private ActorRef selectRandomRoutee() {
        int size = routees.size();
        if (size == 1) {
            return routees.get(0);
        }
        
        int randomIndex = ThreadLocalRandom.current().nextInt(size);
        return routees.get(randomIndex);
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
        return "RandomRouter{" +
                "routees=" + routees.size() +
                '}';
    }
}
