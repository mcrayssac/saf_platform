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

    public DefaultActorSystem(ActorFactory factory) {
        this.factory = factory;
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

        ActorContext context = new ActorContextImpl(ref, null, this);
        actor.preStart(context);

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
            ActorContext context = new ActorContextImpl(instance.ref, null, this);
            instance.actor.postStop(context);
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

    void processMessage(String actorId, Object message, ActorRef sender, CompletableFuture<Object> responseFuture) {
        ActorInstance instance = actors.get(actorId);
        if (instance == null) {
            if (responseFuture != null) {
                responseFuture.completeExceptionally(new IllegalStateException("Acteur introuvable: " + actorId));
            }
            return;
        }

        // Stub Mailbox : on simule l'ajout dans la file
        System.out.println("[MAILBOX STUB] Message ajouté pour acteur " + actorId + ": " + message);

        // Stub Dispatcher : on simule le dispatch (traitement direct pour l'instant)
        System.out.println("[DISPATCHER STUB] Traitement du message pour acteur " + actorId);

        ActorContext context = new ActorContextImpl(instance.ref, sender, this, responseFuture);

        try {
            instance.actor.receive(message, context);
        } catch (Exception e) {
            System.err.println("[DISPATCHER STUB] Erreur lors du traitement: " + e.getMessage());
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
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public void tell(Object message) {
            system.processMessage(id, message, null, null);
        }

        @Override
        public CompletableFuture<Object> ask(Object message, long timeoutMs) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
            system.processMessage(id, message, this, future);
            return future;
        }

        @Override
        public void stop() {
            system.stop(id);
        }
    }

    private static class ActorContextImpl implements ActorContext {
        private final ActorRef self;
        private final ActorRef sender;
        private final ActorSystem system;
        private final CompletableFuture<Object> responseFuture;

        ActorContextImpl(ActorRef self, ActorRef sender, ActorSystem system) {
            this(self, sender, system, null);
        }

        ActorContextImpl(ActorRef self, ActorRef sender, ActorSystem system, CompletableFuture<Object> responseFuture) {
            this.self = self;
            this.sender = sender;
            this.system = system;
            this.responseFuture = responseFuture;
        }

        @Override
        public ActorRef getSelf() {
            return self;
        }

        @Override
        public ActorRef getSender() {
            return sender;
        }

        @Override
        public ActorSystem getSystem() {
            return system;
        }

        @Override
        public void reply(Object response) {
            if (responseFuture != null) {
                responseFuture.complete(response);
            }
        }
    }
}
