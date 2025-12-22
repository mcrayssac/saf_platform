package com.acme.saf.saf_runtime;

import com.acme.saf.actor.core.Mailbox;
import com.acme.saf.actor.core.Message;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implémentation concrète de Mailbox en mémoire avec gestion FIFO et de la concurrence.
 *
 * Caractéristiques :
 * - Stockage en mémoire (pas de persistance)
 * - Ordre FIFO (First In, First Out)
 * - Thread-safe avec ConcurrentLinkedQueue
 * - Support des verrous pour opérations atomiques complexes
 * - Métriques de performance (taille, compteur)
 */
public class InMemoryMailbox implements Mailbox {

    // File d'attente thread-safe pour les messages
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    // Verrou pour les opérations nécessitant atomicité
    private final ReentrantLock lock = new ReentrantLock();

    // Compteurs atomiques pour les métriques
    private final AtomicInteger enqueueCount = new AtomicInteger(0);
    private final AtomicInteger dequeueCount = new AtomicInteger(0);

    @Override
    public void enqueue(Message message) {
        if (message != null) {
            // Utilisation de ConcurrentLinkedQueue pour thread-safety
            messageQueue.offer(message);
            enqueueCount.incrementAndGet();
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
     * Retourne le nombre de messages en attente de traitement.
     */
    public int getPendingCount() {
        return enqueueCount.get() - dequeueCount.get();
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