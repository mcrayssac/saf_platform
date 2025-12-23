package com.acme.saf.actor.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActorPool manages a pool of actor instances with configurable size and routing strategy.
 * Provides dynamic scaling (resize), load balancing, and automatic actor lifecycle management.
 * 
 * The pool can automatically scale based on load or be manually resized.
 * Messages are distributed across pool members using a configurable Router.
 * 
 * Features:
 * - Dynamic pool sizing (min/max bounds)
 * - Multiple routing strategies (round-robin, random, broadcast)
 * - Automatic actor creation and cleanup
 * - Thread-safe operations
 * - Pool metrics and monitoring
 * 
 * Example:
 * <pre>
 * ActorPool pool = new ActorPool.Builder(actorSystem, actorFactory)
 *     .withActorType("WORKER")
 *     .withMinSize(2)
 *     .withMaxSize(10)
 *     .withInitialSize(5)
 *     .withRoutingStrategy(new RoundRobinRouter(...))
 *     .build();
 * 
 * // Route messages to pool
 * pool.route(workMessage, sender);
 * 
 * // Scale dynamically
 * pool.resize(8);
 * 
 * // Auto-scale based on load
 * if (pool.getQueueDepth() > threshold) {
 *     pool.scaleUp();
 * }
 * </pre>
 */
public class ActorPool {
    
    private final ActorSystem actorSystem;
    private final ActorFactory actorFactory;
    private final String actorType;
    private final Map<String, Object> actorParams;
    private final int minSize;
    private final int maxSize;
    private final List<ActorRef> poolMembers;
    private Router router;
    private final AtomicInteger currentSize;
    
    // Metrics
    private final AtomicInteger messagesRouted;
    private final AtomicInteger scaleUpEvents;
    private final AtomicInteger scaleDownEvents;
    
    private ActorPool(Builder builder) {
        this.actorSystem = builder.actorSystem;
        this.actorFactory = builder.actorFactory;
        this.actorType = builder.actorType;
        this.actorParams = builder.actorParams != null ? new HashMap<>(builder.actorParams) : new HashMap<>();
        this.minSize = builder.minSize;
        this.maxSize = builder.maxSize;
        this.poolMembers = Collections.synchronizedList(new ArrayList<>());
        this.currentSize = new AtomicInteger(0);
        this.messagesRouted = new AtomicInteger(0);
        this.scaleUpEvents = new AtomicInteger(0);
        this.scaleDownEvents = new AtomicInteger(0);
        
        // Initialize pool
        initializePool(builder.initialSize);
        
        // Create router with initial members
        if (builder.router != null) {
            this.router = builder.router;
        } else {
            this.router = new RoundRobinRouter(new ArrayList<>(poolMembers));
        }
    }
    
    /**
     * Routes a message to one or more actors in the pool based on the routing strategy.
     * 
     * @param message the message to route
     * @param sender the sender of the message (can be null)
     */
    public void route(Message message, ActorRef sender) {
        if (poolMembers.isEmpty()) {
            throw new IllegalStateException("Actor pool is empty - cannot route message");
        }
        
        router.route(message, sender);
        messagesRouted.incrementAndGet();
    }
    
    /**
     * Resizes the pool to the specified size.
     * Creates new actors if growing, stops actors if shrinking.
     * Respects min/max bounds.
     * 
     * @param newSize the desired pool size
     * @return actual new size after resize
     */
    public synchronized int resize(int newSize) {
        // Enforce bounds
        int targetSize = Math.max(minSize, Math.min(maxSize, newSize));
        int currentCount = currentSize.get();
        
        if (targetSize == currentCount) {
            return currentCount;
        }
        
        if (targetSize > currentCount) {
            // Scale up
            int toAdd = targetSize - currentCount;
            for (int i = 0; i < toAdd; i++) {
                ActorRef newActor = createPoolMember();
                if (newActor != null) {
                    poolMembers.add(newActor);
                    router.addRoutee(newActor);
                    currentSize.incrementAndGet();
                }
            }
            scaleUpEvents.incrementAndGet();
            System.out.println("ActorPool scaled UP: " + currentCount + " → " + currentSize.get());
        } else {
            // Scale down
            int toRemove = currentCount - targetSize;
            for (int i = 0; i < toRemove && !poolMembers.isEmpty(); i++) {
                ActorRef actor = poolMembers.remove(poolMembers.size() - 1);
                router.removeRoutee(actor);
                actor.stop();
                currentSize.decrementAndGet();
            }
            scaleDownEvents.incrementAndGet();
            System.out.println("ActorPool scaled DOWN: " + currentCount + " → " + currentSize.get());
        }
        
        return currentSize.get();
    }
    
    /**
     * Scales up the pool by adding one actor.
     * Respects max size limit.
     * 
     * @return true if scaled up, false if at max capacity
     */
    public boolean scaleUp() {
        int current = currentSize.get();
        if (current >= maxSize) {
            return false;
        }
        resize(current + 1);
        return true;
    }
    
    /**
     * Scales down the pool by removing one actor.
     * Respects min size limit.
     * 
     * @return true if scaled down, false if at min capacity
     */
    public boolean scaleDown() {
        int current = currentSize.get();
        if (current <= minSize) {
            return false;
        }
        resize(current - 1);
        return true;
    }
    
    /**
     * Gets the current size of the pool.
     * 
     * @return number of actors in the pool
     */
    public int size() {
        return currentSize.get();
    }
    
    /**
     * Gets the minimum allowed pool size.
     * 
     * @return minimum size
     */
    public int getMinSize() {
        return minSize;
    }
    
    /**
     * Gets the maximum allowed pool size.
     * 
     * @return maximum size
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Gets all pool members.
     * 
     * @return immutable list of pool members
     */
    public List<ActorRef> getPoolMembers() {
        synchronized (poolMembers) {
            return Collections.unmodifiableList(new ArrayList<>(poolMembers));
        }
    }
    
    /**
     * Gets the total number of messages routed through this pool.
     * 
     * @return message count
     */
    public int getMessagesRouted() {
        return messagesRouted.get();
    }
    
    /**
     * Gets the number of scale-up events.
     * 
     * @return scale-up count
     */
    public int getScaleUpEvents() {
        return scaleUpEvents.get();
    }
    
    /**
     * Gets the number of scale-down events.
     * 
     * @return scale-down count
     */
    public int getScaleDownEvents() {
        return scaleDownEvents.get();
    }
    
    /**
     * Checks if the pool is at minimum capacity.
     * 
     * @return true if at min size
     */
    public boolean isAtMinCapacity() {
        return currentSize.get() <= minSize;
    }
    
    /**
     * Checks if the pool is at maximum capacity.
     * 
     * @return true if at max size
     */
    public boolean isAtMaxCapacity() {
        return currentSize.get() >= maxSize;
    }
    
    /**
     * Gets pool utilization as a percentage.
     * 
     * @return utilization (0.0 to 1.0)
     */
    public double getUtilization() {
        if (maxSize == 0) return 0.0;
        return (double) currentSize.get() / maxSize;
    }
    
    /**
     * Shuts down the pool, stopping all actors.
     */
    public synchronized void shutdown() {
        System.out.println("Shutting down ActorPool with " + currentSize.get() + " actors");
        for (ActorRef actor : poolMembers) {
            try {
                actor.stop();
            } catch (Exception e) {
                System.err.println("Error stopping pool member: " + e.getMessage());
            }
        }
        poolMembers.clear();
        currentSize.set(0);
    }
    
    private void initializePool(int initialSize) {
        int targetSize = Math.max(minSize, Math.min(maxSize, initialSize));
        for (int i = 0; i < targetSize; i++) {
            ActorRef actor = createPoolMember();
            if (actor != null) {
                poolMembers.add(actor);
                currentSize.incrementAndGet();
            }
        }
        System.out.println("ActorPool initialized with " + currentSize.get() + " actors (type: " + actorType + ")");
    }
    
    private ActorRef createPoolMember() {
        try {
            return actorSystem.spawn(actorType, actorParams);
        } catch (Exception e) {
            System.err.println("Failed to create pool member: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "ActorPool{" +
                "type='" + actorType + '\'' +
                ", size=" + currentSize.get() +
                ", min=" + minSize +
                ", max=" + maxSize +
                ", messagesRouted=" + messagesRouted.get() +
                ", utilization=" + String.format("%.1f%%", getUtilization() * 100) +
                '}';
    }
    
    /**
     * Builder for creating ActorPool instances.
     */
    public static class Builder {
        private final ActorSystem actorSystem;
        private final ActorFactory actorFactory;
        private String actorType;
        private Map<String, Object> actorParams;
        private int minSize = 1;
        private int maxSize = 10;
        private int initialSize = 5;
        private Router router;
        
        public Builder(ActorSystem actorSystem, ActorFactory actorFactory) {
            this.actorSystem = actorSystem;
            this.actorFactory = actorFactory;
        }
        
        public Builder withActorType(String actorType) {
            this.actorType = actorType;
            return this;
        }
        
        public Builder withActorParams(Map<String, Object> params) {
            this.actorParams = params;
            return this;
        }
        
        public Builder withMinSize(int minSize) {
            this.minSize = minSize;
            return this;
        }
        
        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder withInitialSize(int initialSize) {
            this.initialSize = initialSize;
            return this;
        }
        
        public Builder withRoutingStrategy(Router router) {
            this.router = router;
            return this;
        }
        
        public ActorPool build() {
            if (actorType == null || actorType.isEmpty()) {
                throw new IllegalArgumentException("Actor type must be specified");
            }
            if (minSize < 0) {
                throw new IllegalArgumentException("Min size cannot be negative");
            }
            if (maxSize < minSize) {
                throw new IllegalArgumentException("Max size must be >= min size");
            }
            return new ActorPool(this);
        }
    }
}
