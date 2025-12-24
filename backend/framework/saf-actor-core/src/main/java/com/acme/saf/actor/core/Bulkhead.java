package com.acme.saf.actor.core;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bulkhead implements the Bulkhead pattern for isolation and resource limiting.
 * Prevents a single failing component from consuming all resources and causing cascade failures.
 * 
 * Features:
 * - Concurrent operation limiting via semaphore
 * - Configurable max concurrent calls
 * - Optional timeout for permit acquisition
 * - Thread-safe operations
 * - Metrics tracking (active, queued, rejected calls)
 * 
 * Use Cases:
 * - Limit concurrent actor operations
 * - Prevent thread pool exhaustion
 * - Isolate failure domains
 * - Rate limiting and throttling
 * 
 * Example:
 * <pre>
 * Bulkhead bulkhead = new Bulkhead.Builder()
 *     .withMaxConcurrentCalls(10)           // Max 10 concurrent operations
 *     .withMaxWaitDuration(Duration.ofSeconds(5))  // Wait up to 5s for permit
 *     .withName("actor-pool")
 *     .build();
 * 
 * try {
 *     bulkhead.execute(() -> {
 *         // Protected operation (max 10 concurrent)
 *         return processMessage();
 *     });
 * } catch (BulkheadFullException e) {
 *     // All permits taken, operation rejected
 * }
 * </pre>
 */
public class Bulkhead {
    
    private final Semaphore semaphore;
    private final Duration maxWaitDuration;
    private final String name;
    
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger rejectedCount = new AtomicInteger(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    
    private Bulkhead(Builder builder) {
        this.semaphore = new Semaphore(builder.maxConcurrentCalls, true);  // Fair semaphore
        this.maxWaitDuration = builder.maxWaitDuration;
        this.name = builder.name;
    }
    
    /**
     * Executes an operation protected by the bulkhead.
     * Acquires a permit before execution, releases after completion.
     * 
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws Exception if the operation fails or bulkhead is full
     */
    public <T> T execute(Operation<T> operation) throws Exception {
        boolean acquired = false;
        
        try {
            // Try to acquire permit
            if (maxWaitDuration != null && maxWaitDuration.toMillis() > 0) {
                acquired = semaphore.tryAcquire(maxWaitDuration.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                acquired = semaphore.tryAcquire();
            }
            
            if (!acquired) {
                rejectedCount.incrementAndGet();
                throw new BulkheadFullException("Bulkhead '" + name + "' is full (max concurrent calls exceeded)");
            }
            
            // Execute operation
            activeCount.incrementAndGet();
            try {
                T result = operation.execute();
                completedCount.incrementAndGet();
                return result;
            } finally {
                activeCount.decrementAndGet();
            }
            
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
    
    /**
     * Executes an operation with explicit timeout.
     * 
     * @param operation the operation to execute
     * @param timeout the timeout duration
     * @param <T> the return type
     * @return the result of the operation
     * @throws Exception if the operation fails or bulkhead is full
     */
    public <T> T execute(Operation<T> operation, Duration timeout) throws Exception {
        boolean acquired = false;
        
        try {
            acquired = semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            
            if (!acquired) {
                rejectedCount.incrementAndGet();
                throw new BulkheadFullException("Bulkhead '" + name + "' is full (timeout exceeded)");
            }
            
            activeCount.incrementAndGet();
            try {
                T result = operation.execute();
                completedCount.incrementAndGet();
                return result;
            } finally {
                activeCount.decrementAndGet();
            }
            
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
    
    /**
     * Tries to acquire a permit without waiting.
     * Useful for fire-and-forget operations.
     * 
     * @return true if permit acquired, false otherwise
     */
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }
    
    /**
     * Releases a permit.
     * Should be called after tryAcquire() when operation completes.
     */
    public void release() {
        semaphore.release();
    }
    
    /**
     * Gets the number of available permits.
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }
    
    /**
     * Gets the number of threads waiting for permits.
     */
    public int getQueueLength() {
        return semaphore.getQueueLength();
    }
    
    /**
     * Gets the number of currently active operations.
     */
    public int getActiveCount() {
        return activeCount.get();
    }
    
    /**
     * Gets the number of rejected operations.
     */
    public int getRejectedCount() {
        return rejectedCount.get();
    }
    
    /**
     * Gets the number of completed operations.
     */
    public int getCompletedCount() {
        return completedCount.get();
    }
    
    /**
     * Gets the bulkhead name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Resets metrics counters.
     */
    public void resetMetrics() {
        rejectedCount.set(0);
        completedCount.set(0);
    }
    
    /**
     * Functional interface for operations protected by bulkhead.
     */
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Exception thrown when bulkhead is full.
     */
    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) {
            super(message);
        }
    }
    
    /**
     * Builder for creating Bulkhead instances.
     */
    public static class Builder {
        private int maxConcurrentCalls = 10;
        private Duration maxWaitDuration = Duration.ZERO;
        private String name = "default";
        
        public Builder withMaxConcurrentCalls(int maxCalls) {
            if (maxCalls <= 0) {
                throw new IllegalArgumentException("Max concurrent calls must be positive");
            }
            this.maxConcurrentCalls = maxCalls;
            return this;
        }
        
        public Builder withMaxWaitDuration(Duration duration) {
            this.maxWaitDuration = duration;
            return this;
        }
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Bulkhead build() {
            return new Bulkhead(this);
        }
    }
}
