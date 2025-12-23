package com.acme.saf.saf_runtime;

import com.acme.saf.actor.core.Mailbox;
import com.acme.saf.actor.core.Message;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Implémentation concrète de Mailbox en mémoire avec gestion FIFO et de la concurrence.
 *
 * Caractéristiques :
 * - Stockage en mémoire (pas de persistance)
 * - Ordre FIFO (First In, First Out)
 * - Thread-safe avec ConcurrentLinkedQueue
 * - Support des verrous pour opérations atomiques complexes
 * - Métriques de performance (taille, compteur)
 * - Backpressure support (size limits, drop strategies)
 * - Dead letter handling for rejected messages
 */
public class InMemoryMailbox implements Mailbox {

    // File d'attente thread-safe pour les messages
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    // Verrou pour les opérations nécessitant atomicité
    private final ReentrantLock lock = new ReentrantLock();

    // Compteurs atomiques pour les métriques
    private final AtomicInteger enqueueCount = new AtomicInteger(0);
    private final AtomicInteger dequeueCount = new AtomicInteger(0);
    private final AtomicInteger droppedCount = new AtomicInteger(0);
    
    // Backpressure configuration
    private int maxSize = Integer.MAX_VALUE;  // Default: unlimited
    private DropStrategy dropStrategy = DropStrategy.DROP_OLDEST;
    private Consumer<Message> deadLetterHandler = null;

    /**
     * Drop strategy for backpressure handling.
     */
    public enum DropStrategy {
        DROP_OLDEST,    // Remove oldest message when full
        DROP_NEWEST,    // Reject new message when full
        DROP_ALL        // Clear all messages when full
    }
    
    @Override
    public void enqueue(Message message) {
        if (message == null) {
            return;
        }
        
        // Check backpressure limit
        if (size() >= maxSize) {
            handleBackpressure(message);
            return;
        }
        
        // Utilisation de ConcurrentLinkedQueue pour thread-safety
        messageQueue.offer(message);
        enqueueCount.incrementAndGet();
    }
    
    /**
     * Handles backpressure when mailbox is full.
     */
    private void handleBackpressure(Message newMessage) {
        lock.lock();
        try {
            switch (dropStrategy) {
                case DROP_OLDEST:
                    // Remove oldest message, add new one
                    Message dropped = messageQueue.poll();
                    if (dropped != null) {
                        droppedCount.incrementAndGet();
                        sendToDeadLetter(dropped);
                    }
                    messageQueue.offer(newMessage);
                    enqueueCount.incrementAndGet();
                    break;
                    
                case DROP_NEWEST:
                    // Reject new message
                    droppedCount.incrementAndGet();
                    sendToDeadLetter(newMessage);
                    break;
                    
                case DROP_ALL:
                    // Clear all, add new one
                    int cleared = messageQueue.size();
                    messageQueue.forEach(this::sendToDeadLetter);
                    messageQueue.clear();
                    droppedCount.addAndGet(cleared);
                    messageQueue.offer(newMessage);
                    enqueueCount.incrementAndGet();
                    break;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Sends dropped message to dead letter handler.
     */
    private void sendToDeadLetter(Message message) {
        if (deadLetterHandler != null) {
            try {
                deadLetterHandler.accept(message);
            } catch (Exception e) {
                // Ignore dead letter handler failures
                System.err.println("Dead letter handler failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Message dequeue() {
        // ConcurrentLinkedQueue garantit la thread-safety
        Message message = messageQueue.poll();
        if (message != null) {
            dequeueCount.incrementAndGet();
        }
        return message;
    }

    @Override
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }

    @Override
    public int size() {
        return messageQueue.size();
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            messageQueue.clear();
            enqueueCount.set(0);
            dequeueCount.set(0);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retourne le nombre total de messages enqueued.
     */
    public int getEnqueueCount() {
        return enqueueCount.get();
    }

    /**
     * Retourne le nombre total de messages dequeued.
     */
    public int getDequeueCount() {
        return dequeueCount.get();
    }

    /**
     * Returns the number of dropped messages (backpressure).
     */
    public int getDroppedCount() {
        return droppedCount.get();
    }
    
    /**
     * Retourne le nombre de messages en attente de traitement.
     */
    public int getPendingCount() {
        return enqueueCount.get() - dequeueCount.get();
    }
    
    /**
     * Sets the maximum mailbox size for backpressure.
     * 
     * @param maxSize maximum number of messages (Integer.MAX_VALUE for unlimited)
     */
    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }
        this.maxSize = maxSize;
    }
    
    /**
     * Gets the maximum mailbox size.
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Sets the drop strategy for backpressure handling.
     * 
     * @param strategy the drop strategy to use
     */
    public void setDropStrategy(DropStrategy strategy) {
        this.dropStrategy = strategy;
    }
    
    /**
     * Gets the current drop strategy.
     */
    public DropStrategy getDropStrategy() {
        return dropStrategy;
    }
    
    /**
     * Sets the dead letter handler for dropped messages.
     * 
     * @param handler consumer that receives dropped messages
     */
    public void setDeadLetterHandler(Consumer<Message> handler) {
        this.deadLetterHandler = handler;
    }
    
    /**
     * Checks if mailbox is at capacity.
     */
    public boolean isFull() {
        return size() >= maxSize;
    }
    
    /**
     * Gets the utilization rate (0.0 to 1.0).
     */
    public double getUtilization() {
        if (maxSize == Integer.MAX_VALUE) {
            return 0.0;
        }
        return (double) size() / maxSize;
    }

    /**
     * Vérifie si le verrou est actuellement détenu.
     */
    public boolean isLocked() {
        return lock.isLocked();
    }

    /**
     * Effectue une opération atomique avec verrouillage.
     * Utile pour les opérations complexes nécessitant cohérence.
     */
    public <T> T withLock(java.util.function.Supplier<T> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Effectue une opération atomique avec verrouillage (void).
     */
    public void withLock(java.lang.Runnable operation) {
        lock.lock();
        try {
            operation.run();
        } finally {
            lock.unlock();
        }
    }
}
