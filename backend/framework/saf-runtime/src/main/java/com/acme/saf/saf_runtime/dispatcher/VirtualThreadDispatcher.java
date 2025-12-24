package com.acme.saf.saf_runtime.dispatcher;

import com.acme.saf.actor.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class VirtualThreadDispatcher implements Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadDispatcher.class);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Nombre de messages traités par un acteur
    private static final int THROUGHPUT_LIMIT = 50;

    @Override
    public void dispatch(ActorRef actorRef, Mailbox mailbox, Actor actor, SupervisionStrategy supervisionStrategy) {
        // On soumet la tâche de traitement à un thread virtuel
        executor.submit(() -> processMailbox(actorRef, mailbox, actor, supervisionStrategy));
    }

    private void processMailbox(ActorRef actorRef, Mailbox mailbox, Actor actor, SupervisionStrategy supervisionStrategy) {
        try {
            int processedCount = 0;

            // Boucle de consommation
            while (processedCount < THROUGHPUT_LIMIT) {
                // Récupérer le prochain message
                Message message = mailbox.dequeue();

                if (message == null) {
                    return; // La boîte est vide donc le thread s'arrête
                }

                // Traitement du message par l'acteur
                try {
                    actor.receive(message);
                } catch (Exception cause) {
                    log.error("Error processing message for actor {}", actorRef.getActorId(), cause);
                    
                    if (supervisionStrategy != null) {
                        // La stratégie décide quoi faire (Restart, Stop...)
                        supervisionStrategy.handleFailure(actor, cause, message);
                    }
                }

                processedCount++;
            }

            // Si on a atteint la limite mais qu'il reste des messages on se replanifie
            if (!mailbox.isEmpty()) {
                dispatch(actorRef, mailbox, actor, supervisionStrategy);
            }

        } catch (Exception e) {
            log.error("Critical dispatcher error for actor {}", actorRef.getActorId(), e);
        }
    }

    // Implémentation des méthodes de cycle de vie de l'executor

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
}