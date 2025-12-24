package com.acme.saf.actor.core;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CircuitBreaker implements the Circuit Breaker pattern to prevent cascade failures.
 * Monitors failure rates and temporarily blocks operations when threshold is exceeded.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failure threshold exceeded, requests fail fast
 * - HALF_OPEN: Testing if service recovered, limited requests allowed
 * 
 * Features:
 * - Automatic state transitions based on failure rates
 * - Configurable thresholds and timeouts
 * - Thread-safe atomic operations
 * - Metrics tracking (success/failure counts)
 * - Recovery testing in half-open state
 * 
 * Example:
 * <pre>
 * CircuitBreaker breaker = new CircuitBreaker.Builder()
 *     .withFailureThreshold(5)              // Open after 5 failures
 *     .withFailureRateThreshold(0.5)        // Or 50% failure rate
 *     .withTimeout(Duration.ofSeconds(60))  // Stay open for 60s
 *     .withHalfOpenRequests(3)              // Test with 3 requests
 *     .build();
 * 
 * try {
 *     breaker.execute(() -> {
 *         // Protected operation
 *         return callRemoteService();
 *     });
 * } catch (CircuitBreakerOpenException e) {
 *     // Circuit is open, fail fast
 * }
 * </pre>
 */
public class CircuitBreaker {
    
    /**
     * Circuit breaker states.
     */
    public enum State {
        CLOSED,      // Normal operation
        OPEN,        // Blocking requests
        HALF_OPEN    // Testing recovery
    }
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
    
    private final int failureThreshold;
    private final double failureRateThreshold;
    private final int minimumRequests;
    private final Duration timeout;
    private final int halfOpenMaxRequests;
    private final String name;
    
    private CircuitBreaker(Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.failureRateThreshold = builder.failureRateThreshold;
        this.minimumRequests = builder.minimumRequests;
        this.timeout = builder.timeout;
        this.halfOpenMaxRequests = builder.halfOpenMaxRequests;
        this.name = builder.name;
    }
    
    /**
     * Executes an operation protected by the circuit breaker.
     * 
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws Exception if the operation fails or circuit is open
     */
    public <T> T execute(Operation<T> operation) throws Exception {
        // Check if circuit should transition from OPEN to HALF_OPEN
        if (state.get() == State.OPEN && shouldAttemptReset()) {
            state.set(State.HALF_OPEN);
            halfOpenAttempts.set(0);
        }
        
        // Fail fast if circuit is open
        if (state.get() == State.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is OPEN");
        }
        
        // Limit requests in half-open state
        if (state.get() == State.HALF_OPEN) {
            int attempts = halfOpenAttempts.incrementAndGet();
            if (attempts > halfOpenMaxRequests) {
                halfOpenAttempts.decrementAndGet();
                throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is HALF_OPEN (max requests exceeded)");
            }
        }
        
        try {
            T result = operation.execute();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    /**
     * Records a successful operation.
     */
    private void onSuccess() {
        successCount.incrementAndGet();
        
        if (state.get() == State.HALF_OPEN) {
            // If enough successes in half-open, transition to closed
            if (halfOpenAttempts.get() >= halfOpenMaxRequests) {
                reset();
            }
        }
    }
    
    /**
     * Records a failed operation.
     */
    private void onFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (state.get() == State.HALF_OPEN) {
            // Any failure in half-open goes back to open
            tripBreaker();
        } else if (state.get() == State.CLOSED) {
            // Check if we should trip the breaker
            if (shouldTrip()) {
                tripBreaker();
            }
        }
    }
    
    /**
     * Determines if the circuit breaker should trip (open).
     */
    private boolean shouldTrip() {
        int failures = failureCount.get();
        int successes = successCount.get();
        int total = failures + successes;
        
        // Check absolute failure threshold
        if (failures >= failureThreshold) {
            return true;
        }
        
        // Check failure rate threshold (if minimum requests met)
        if (total >= minimumRequests) {
            double failureRate = (double) failures / total;
            return failureRate >= failureRateThreshold;
        }
        
        return false;
    }
    
    /**
     * Determines if circuit should attempt reset from OPEN to HALF_OPEN.
     */
    private boolean shouldAttemptReset() {
        long lastFailure = lastFailureTime.get();
        if (lastFailure == 0) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - lastFailure;
        return elapsed >= timeout.toMillis();
    }
    
    /**
     * Trips the circuit breaker (OPEN state).
     */
    private void tripBreaker() {
        state.set(State.OPEN);
        System.out.println("Circuit breaker '" + name + "' OPENED (failures=" + failureCount.get() + ")");
    }
    
    /**
     * Resets the circuit breaker (CLOSED state).
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        halfOpenAttempts.set(0);
        System.out.println("Circuit breaker '" + name + "' CLOSED (reset)");
    }
    
    /**
     * Forces the circuit breaker to OPEN state.
     */
    public void forceOpen() {
        state.set(State.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
    }
    
    /**
     * Gets the current state of the circuit breaker.
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Gets the current failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets the current success count.
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * Gets the current failure rate.
     */
    public double getFailureRate() {
        int total = failureCount.get() + successCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failureCount.get() / total;
    }
    
    /**
     * Gets the circuit breaker name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Functional interface for operations protected by circuit breaker.
     */
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Exception thrown when circuit breaker is open.
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
    
    /**
     * Builder for creating CircuitBreaker instances.
     */
    public static class Builder {
        private int failureThreshold = 5;
        private double failureRateThreshold = 0.5;
        private int minimumRequests = 10;
        private Duration timeout = Duration.ofSeconds(60);
        private int halfOpenMaxRequests = 3;
        private String name = "default";
        
        public Builder withFailureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }
        
        public Builder withFailureRateThreshold(double rate) {
            if (rate < 0.0 || rate > 1.0) {
                throw new IllegalArgumentException("Failure rate must be between 0.0 and 1.0");
            }
            this.failureRateThreshold = rate;
            return this;
        }
        
        public Builder withMinimumRequests(int minimum) {
            this.minimumRequests = minimum;
            return this;
        }
        
        public Builder withTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder withHalfOpenRequests(int requests) {
            this.halfOpenMaxRequests = requests;
            return this;
        }
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public CircuitBreaker build() {
            return new CircuitBreaker(this);
        }
    }
}
