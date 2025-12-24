package com.acme.saf.actor.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AutoScaler automatically scales an ActorPool based on load metrics.
 * Monitors pool utilization and scales up/down based on configurable thresholds.
 * 
 * Scaling decisions are based on:
 * - Message throughput (messages per second)
 * - Pool utilization (current size vs max size)
 * - Custom metrics (optional)
 * 
 * Features:
 * - Periodic load monitoring
 * - Configurable scale-up/down thresholds
 * - Cooldown periods to prevent oscillation
 * - Thread-safe operation
 * - Graceful shutdown
 * 
 * Example:
 * <pre>
 * AutoScaler scaler = new AutoScaler.Builder(actorPool)
 *     .withScaleUpThreshold(0.8)      // Scale up at 80% utilization
 *     .withScaleDownThreshold(0.3)    // Scale down at 30% utilization
 *     .withCheckInterval(5, TimeUnit.SECONDS)
 *     .withCooldownPeriod(30, TimeUnit.SECONDS)
 *     .build();
 * 
 * scaler.start();  // Begin auto-scaling
 * 
 * // Later...
 * scaler.stop();   // Stop auto-scaling
 * </pre>
 */
public class AutoScaler {
    
    private final ActorPool pool;
    private final double scaleUpThreshold;
    private final double scaleDownThreshold;
    private final long checkIntervalMs;
    private final long cooldownPeriodMs;
    private final int scaleUpIncrement;
    private final int scaleDownDecrement;
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;
    private volatile long lastScaleUpTime;
    private volatile long lastScaleDownTime;
    private volatile int lastMessagesRouted;
    
    private AutoScaler(Builder builder) {
        this.pool = builder.pool;
        this.scaleUpThreshold = builder.scaleUpThreshold;
        this.scaleDownThreshold = builder.scaleDownThreshold;
        this.checkIntervalMs = builder.checkIntervalMs;
        this.cooldownPeriodMs = builder.cooldownPeriodMs;
        this.scaleUpIncrement = builder.scaleUpIncrement;
        this.scaleDownDecrement = builder.scaleDownDecrement;
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoScaler-" + pool);
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.lastScaleUpTime = 0;
        this.lastScaleDownTime = 0;
        this.lastMessagesRouted = 0;
    }
    
    /**
     * Starts the auto-scaler.
     * Begins periodic monitoring and scaling decisions.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            System.out.println("AutoScaler started for pool: " + pool);
            lastMessagesRouted = pool.getMessagesRouted();
            
            scheduler.scheduleAtFixedRate(
                this::checkAndScale,
                checkIntervalMs,
                checkIntervalMs,
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * Stops the auto-scaler.
     * Stops monitoring but does not affect the pool.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("AutoScaler stopped for pool: " + pool);
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Checks if the auto-scaler is currently running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Gets the current pool utilization.
     * 
     * @return utilization (0.0 to 1.0)
     */
    public double getCurrentUtilization() {
        return pool.getUtilization();
    }
    
    /**
     * Gets the message throughput (messages per second since last check).
     * 
     * @return messages per second
     */
    public double getMessageThroughput() {
        int currentMessages = pool.getMessagesRouted();
        int messagesDelta = currentMessages - lastMessagesRouted;
        double intervalSeconds = checkIntervalMs / 1000.0;
        return messagesDelta / intervalSeconds;
    }
    
    private void checkAndScale() {
        try {
            double utilization = pool.getUtilization();
            long now = System.currentTimeMillis();
            
            // Check if we should scale up
            if (shouldScaleUp(utilization, now)) {
                scaleUp();
                lastScaleUpTime = now;
            }
            // Check if we should scale down
            else if (shouldScaleDown(utilization, now)) {
                scaleDown();
                lastScaleDownTime = now;
            }
            
            // Update metrics
            lastMessagesRouted = pool.getMessagesRouted();
            
        } catch (Exception e) {
            System.err.println("Error in AutoScaler: " + e.getMessage());
        }
    }
    
    private boolean shouldScaleUp(double utilization, long now) {
        // Don't scale if at max capacity
        if (pool.isAtMaxCapacity()) {
            return false;
        }
        
        // Check cooldown period
        if (now - lastScaleUpTime < cooldownPeriodMs) {
            return false;
        }
        
        // Scale up if utilization exceeds threshold
        return utilization >= scaleUpThreshold;
    }
    
    private boolean shouldScaleDown(double utilization, long now) {
        // Don't scale if at min capacity
        if (pool.isAtMinCapacity()) {
            return false;
        }
        
        // Check cooldown period
        if (now - lastScaleDownTime < cooldownPeriodMs) {
            return false;
        }
        
        // Scale down if utilization below threshold
        return utilization <= scaleDownThreshold;
    }
    
    private void scaleUp() {
        int currentSize = pool.size();
        int targetSize = Math.min(pool.getMaxSize(), currentSize + scaleUpIncrement);
        
        System.out.println(String.format(
            "AutoScaler: Scaling UP pool from %d to %d (utilization: %.1f%%)",
            currentSize, targetSize, getCurrentUtilization() * 100
        ));
        
        pool.resize(targetSize);
    }
    
    private void scaleDown() {
        int currentSize = pool.size();
        int targetSize = Math.max(pool.getMinSize(), currentSize - scaleDownDecrement);
        
        System.out.println(String.format(
            "AutoScaler: Scaling DOWN pool from %d to %d (utilization: %.1f%%)",
            currentSize, targetSize, getCurrentUtilization() * 100
        ));
        
        pool.resize(targetSize);
    }
    
    @Override
    public String toString() {
        return "AutoScaler{" +
                "pool=" + pool +
                ", scaleUpThreshold=" + scaleUpThreshold +
                ", scaleDownThreshold=" + scaleDownThreshold +
                ", running=" + running.get() +
                ", utilization=" + String.format("%.1f%%", getCurrentUtilization() * 100) +
                '}';
    }
    
    /**
     * Builder for creating AutoScaler instances.
     */
    public static class Builder {
        private final ActorPool pool;
        private double scaleUpThreshold = 0.75;      // 75% utilization
        private double scaleDownThreshold = 0.25;    // 25% utilization
        private long checkIntervalMs = 10000;        // 10 seconds
        private long cooldownPeriodMs = 30000;       // 30 seconds
        private int scaleUpIncrement = 1;            // Add 1 actor at a time
        private int scaleDownDecrement = 1;          // Remove 1 actor at a time
        
        public Builder(ActorPool pool) {
            if (pool == null) {
                throw new IllegalArgumentException("ActorPool cannot be null");
            }
            this.pool = pool;
        }
        
        /**
         * Sets the utilization threshold for scaling up.
         * When pool utilization exceeds this threshold, scale up occurs.
         * 
         * @param threshold utilization threshold (0.0 to 1.0)
         * @return this builder
         */
        public Builder withScaleUpThreshold(double threshold) {
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
            }
            this.scaleUpThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the utilization threshold for scaling down.
         * When pool utilization falls below this threshold, scale down occurs.
         * 
         * @param threshold utilization threshold (0.0 to 1.0)
         * @return this builder
         */
        public Builder withScaleDownThreshold(double threshold) {
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
            }
            this.scaleDownThreshold = threshold;
            return this;
        }
        
        /**
         * Sets how often to check pool metrics and make scaling decisions.
         * 
         * @param interval check interval
         * @param unit time unit
         * @return this builder
         */
        public Builder withCheckInterval(long interval, TimeUnit unit) {
            this.checkIntervalMs = unit.toMillis(interval);
            return this;
        }
        
        /**
         * Sets the cooldown period between scaling operations.
         * Prevents rapid oscillation of pool size.
         * 
         * @param period cooldown period
         * @param unit time unit
         * @return this builder
         */
        public Builder withCooldownPeriod(long period, TimeUnit unit) {
            this.cooldownPeriodMs = unit.toMillis(period);
            return this;
        }
        
        /**
         * Sets how many actors to add during scale-up.
         * 
         * @param increment number of actors to add
         * @return this builder
         */
        public Builder withScaleUpIncrement(int increment) {
            if (increment < 1) {
                throw new IllegalArgumentException("Scale up increment must be at least 1");
            }
            this.scaleUpIncrement = increment;
            return this;
        }
        
        /**
         * Sets how many actors to remove during scale-down.
         * 
         * @param decrement number of actors to remove
         * @return this builder
         */
        public Builder withScaleDownDecrement(int decrement) {
            if (decrement < 1) {
                throw new IllegalArgumentException("Scale down decrement must be at least 1");
            }
            this.scaleDownDecrement = decrement;
            return this;
        }
        
        public AutoScaler build() {
            if (scaleUpThreshold <= scaleDownThreshold) {
                throw new IllegalArgumentException(
                    "Scale up threshold must be greater than scale down threshold"
                );
            }
            return new AutoScaler(this);
        }
    }
}
