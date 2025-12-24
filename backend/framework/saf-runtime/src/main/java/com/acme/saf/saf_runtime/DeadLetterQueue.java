package com.acme.saf.saf_runtime;

import com.acme.saf.actor.core.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeadLetterQueue stores undeliverable or rejected messages.
 * Provides analysis, replay, and diagnostic capabilities.
 * 
 * Features:
 * - Thread-safe message storage
 * - Message metadata (reason, timestamp, actor ID)
 * - Configurable size limits
 * - Message replay capability
 * - Statistics and diagnostics
 * 
 * Use Cases:
 * - Undeliverable messages (actor not found)
 * - Rejected messages (mailbox full, backpressure)
 * - Failed message processing
 * - Poison pill messages (repeated failures)
 * 
 * Example:
 * <pre>
 * DeadLetterQueue dlq = new DeadLetterQueue(1000);
 * 
 * // Add failed message
 * dlq.add(message, actorId, "Mailbox full");
 * 
 * // Get statistics
 * int count = dlq.size();
 * Map<String, Integer> stats = dlq.getStatisticsByReason();
 * 
 * // Replay messages
 * List<DeadLetter> toReplay = dlq.getAll();
 * toReplay.forEach(dl -> actor.tell(dl.getMessage()));
 * dlq.clear();
 * </pre>
 */
public class DeadLetterQueue {
    
    /**
     * DeadLetter wraps a message with diagnostic information.
     */
    public static class DeadLetter {
        private final Message message;
        private final String actorId;
        private final String reason;
        private final Instant timestamp;
        private final String correlationId;
        
        public DeadLetter(Message message, String actorId, String reason, String correlationId) {
            this.message = message;
            this.actorId = actorId;
            this.reason = reason;
            this.timestamp = Instant.now();
            this.correlationId = correlationId;
        }
        
        public Message getMessage() { return message; }
        public String getActorId() { return actorId; }
        public String getReason() { return reason; }
        public Instant getTimestamp() { return timestamp; }
        public String getCorrelationId() { return correlationId; }
        
        @Override
        public String toString() {
            return String.format("DeadLetter{actorId='%s', reason='%s', timestamp=%s, correlationId='%s'}",
                    actorId, reason, timestamp, correlationId);
        }
    }
    
    private final ConcurrentLinkedQueue<DeadLetter> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicInteger droppedCount = new AtomicInteger(0);
    private final int maxSize;
    
    // Statistics
    private final Map<String, AtomicInteger> reasonCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> actorCounts = new ConcurrentHashMap<>();
    
    /**
     * Creates a DeadLetterQueue with specified maximum size.
     * 
     * @param maxSize maximum number of messages to store
     */
    public DeadLetterQueue(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }
        this.maxSize = maxSize;
    }
    
    /**
     * Creates a DeadLetterQueue with default size (1000).
     */
    public DeadLetterQueue() {
        this(1000);
    }
    
    /**
     * Adds a message to the dead letter queue.
     * 
     * @param message the undeliverable message
     * @param actorId the target actor ID
     * @param reason the reason for failure
     */
    public void add(Message message, String actorId, String reason) {
        add(message, actorId, reason, null);
    }
    
    /**
     * Adds a message to the dead letter queue with correlation ID.
     * 
     * @param message the undeliverable message
     * @param actorId the target actor ID
     * @param reason the reason for failure
     * @param correlationId correlation ID for tracing
     */
    public void add(Message message, String actorId, String reason, String correlationId) {
        if (message == null) {
            return;
        }
        
        // Check size limit
        if (queue.size() >= maxSize) {
            // Drop oldest message
            DeadLetter dropped = queue.poll();
            if (dropped != null) {
                droppedCount.incrementAndGet();
            }
        }
        
        // Add new dead letter
        DeadLetter deadLetter = new DeadLetter(message, actorId, reason, correlationId);
        queue.offer(deadLetter);
        totalCount.incrementAndGet();
        
        // Update statistics
        reasonCounts.computeIfAbsent(reason, k -> new AtomicInteger(0)).incrementAndGet();
        if (actorId != null) {
            actorCounts.computeIfAbsent(actorId, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        System.err.println("Dead letter: " + deadLetter);
    }
    
    /**
     * Gets all dead letters.
     * 
     * @return list of all dead letters
     */
    public List<DeadLetter> getAll() {
        return new ArrayList<>(queue);
    }
    
    /**
     * Gets dead letters for a specific actor.
     * 
     * @param actorId the actor ID to filter by
     * @return list of dead letters for the actor
     */
    public List<DeadLetter> getByActorId(String actorId) {
        return queue.stream()
                .filter(dl -> actorId.equals(dl.getActorId()))
                .toList();
    }
    
    /**
     * Gets dead letters by reason.
     * 
     * @param reason the failure reason to filter by
     * @return list of dead letters with the reason
     */
    public List<DeadLetter> getByReason(String reason) {
        return queue.stream()
                .filter(dl -> reason.equals(dl.getReason()))
                .toList();
    }
    
    /**
     * Gets dead letters by correlation ID.
     * 
     * @param correlationId the correlation ID to filter by
     * @return list of dead letters with the correlation ID
     */
    public List<DeadLetter> getByCorrelationId(String correlationId) {
        return queue.stream()
                .filter(dl -> correlationId.equals(dl.getCorrelationId()))
                .toList();
    }
    
    /**
     * Gets the current size of the queue.
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Gets the total number of dead letters (including dropped).
     */
    public int getTotalCount() {
        return totalCount.get();
    }
    
    /**
     * Gets the number of dropped dead letters (overflow).
     */
    public int getDroppedCount() {
        return droppedCount.get();
    }
    
    /**
     * Gets statistics by failure reason.
     * 
     * @return map of reason to count
     */
    public Map<String, Integer> getStatisticsByReason() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        reasonCounts.forEach((reason, count) -> stats.put(reason, count.get()));
        return stats;
    }
    
    /**
     * Gets statistics by actor ID.
     * 
     * @return map of actor ID to count
     */
    public Map<String, Integer> getStatisticsByActor() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        actorCounts.forEach((actorId, count) -> stats.put(actorId, count.get()));
        return stats;
    }
    
    /**
     * Gets the most common failure reason.
     * 
     * @return the most common reason, or null if empty
     */
    public String getMostCommonReason() {
        return reasonCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.get(), b.get())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * Gets the actor with most failures.
     * 
     * @return the actor ID with most failures, or null if empty
     */
    public String getMostFailedActor() {
        return actorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.get(), b.get())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * Clears all dead letters and resets statistics.
     */
    public void clear() {
        queue.clear();
        reasonCounts.clear();
        actorCounts.clear();
    }
    
    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Checks if the queue is full.
     */
    public boolean isFull() {
        return queue.size() >= maxSize;
    }
    
    /**
     * Gets the utilization rate (0.0 to 1.0).
     */
    public double getUtilization() {
        return (double) queue.size() / maxSize;
    }
    
    /**
     * Prints diagnostics to standard output.
     */
    public void printDiagnostics() {
        System.out.println("=== Dead Letter Queue Diagnostics ===");
        System.out.println("Current size: " + size());
        System.out.println("Total count: " + getTotalCount());
        System.out.println("Dropped count: " + getDroppedCount());
        System.out.println("Utilization: " + String.format("%.2f%%", getUtilization() * 100));
        
        System.out.println("\nFailure Reasons:");
        getStatisticsByReason().forEach((reason, count) ->
                System.out.println("  " + reason + ": " + count));
        
        System.out.println("\nTop Failed Actors:");
        getStatisticsByActor().entrySet().stream()
                .sorted(Map.Entry.comparingByValue((a, b) -> Integer.compare(b, a)))
                .limit(5)
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
        
        System.out.println("=====================================");
    }
}
