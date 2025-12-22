package com.acme.saf.saf_runtime;

import com.acme.saf.actor.core.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultActorSystem implements ActorSystem {

    private final ActorFactory factory;
    private final Map<String, ActorInstance> actors = new ConcurrentHashMap<>();
    private final Mailbox mailbox;

    public DefaultActorSystem(ActorFactory factory) {
        this(factory, new InMemoryMailbox());
    }

    public DefaultActorSystem(ActorFactory factory, Mailbox mailbox) {
        this.factory = factory;
        this.mailbox = mailbox;
    }

    @Override
    public ActorRef spawn(String type, Map<String, Object> params) {
        String id = UUID.randomUUID().toString();

        Actor actor = factory.create(type, params);
        if (actor == null) {
            throw new IllegalArgumentException("Type d'acteur non supporté: " + type);
        }

        ActorRefImpl ref = new ActorRefImpl(id, type, this);
        ActorInstance instance = new ActorInstance(actor, ref);
        actors.put(id, instance);

        try {
            actor.preStart();
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de l'acteur " + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Acteur créé: " + id + " (type: " + type + ")");

        return ref;
    }

    @Override
    public ActorRef getActor(String id) {
        ActorInstance instance = actors.get(id);
        return instance != null ? instance.ref : null;
    }

    @Override
    public void stop(String id) {
        ActorInstance instance = actors.remove(id);
        if (instance != null) {
            try {
                instance.actor.postStop();
            } catch (Exception e) {
                System.err.println("Erreur lors de l'arrêt de l'acteur " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Acteur arrêté: " + id);
        }
    }

    @Override
    public void shutdown() {
        actors.keySet().forEach(this::stop);
        System.out.println("ActorSystem arrêté");
    }

    @Override
    public boolean hasActor(String id) {
        return actors.containsKey(id);
    }

    void processMessage(String actorId, Message message, ActorRef sender, CompletableFuture<Object> responseFuture) {
        ActorInstance instance = actors.get(actorId);
        if (instance == null) {
            if (responseFuture != null) {
                responseFuture.completeExceptionally(new IllegalStateException("Acteur introuvable: " + actorId));
            }
            return;
        }

        // Mailbox : ajout + lecture FIFO
        mailbox.enqueue(message);
        System.out.println("[MAILBOX] Message ajouté pour acteur " + actorId + ": " + message);

        // Dispatcher : traitement direct pour l'instant
        System.out.println("[DISPATCHER] Traitement du message pour acteur " + actorId);
        Message next = mailbox.dequeue();
        if (next == null) {
            return;
        }

        try {
            instance.actor.receive(next);
        } catch (Exception e) {
            System.err.println("[DISPATCHER] Erreur lors du traitement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ActorInstance {
        final Actor actor;
        final ActorRefImpl ref;

        ActorInstance(Actor actor, ActorRefImpl ref) {
            this.actor = actor;
            this.ref = ref;
        }
    }

    private class ActorRefImpl implements ActorRef {
        private final String id;
        private final String type;
        private final DefaultActorSystem system;

        ActorRefImpl(String id, String type, DefaultActorSystem system) {
            this.id = id;
            this.type = type;
            this.system = system;
        }

        @Override
        public String getActorId() {
            return id;
        }

        @Override
        public String getPath() {
            return "/user/" + id;
        }

        @Override
        public void tell(Message message) {
            system.processMessage(id, message, null, null);
        }

        @Override
        public void tell(Message message, ActorRef sender) {
            system.processMessage(id, message, sender, null);
        }

        @Override
        public CompletableFuture<Object> ask(Message message, long timeout, TimeUnit unit) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.orTimeout(timeout, unit);
            system.processMessage(id, message, this, future);
            return future;
        }

        @Override
        public void forward(Message message, ActorRef originalSender) {
            system.processMessage(id, message, originalSender, null);
        }

        @Override
        public boolean isActive() {
            return system.hasActor(id);
        }

        @Override
        public void stop() {
            system.stop(id);
        }
    }
}
