package com.acme.saf.saf_runtime;

import com.acme.saf.actor.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultActorSystem implements ActorSystem {

    private final ActorFactory factory;
    private final Map<String, ActorInstance> actors = new ConcurrentHashMap<>();
    private final Mailbox mailbox;
    private WebSocketMessageSender webSocketSender;  // Optional WebSocket support

    public DefaultActorSystem(ActorFactory factory) {
        this(factory, new InMemoryMailbox());
    }

    public DefaultActorSystem(ActorFactory factory, Mailbox mailbox) {
        this.factory = factory;
        this.mailbox = mailbox;
    }
    
    /**
     * Set WebSocket sender for actor-to-client communication.
     * This is optional - if not set, actors won't have WebSocket support.
     */
    public void setWebSocketSender(WebSocketMessageSender sender) {
        this.webSocketSender = sender;
    }

    @Override
    public ActorRef spawn(String type, Map<String, Object> params) {
        String id = UUID.randomUUID().toString();
        return spawn(type, id, params);
    }

    @Override
    public ActorRef spawn(String type, String id, Map<String, Object> params) {
        Actor actor = factory.create(type, params);
        if (actor == null) {
            throw new IllegalArgumentException("Type d'acteur non supporté: " + type);
        }

        ActorRefImpl ref = new ActorRefImpl(id, type, this);
        ActorInstance instance = new ActorInstance(actor, ref);
        actors.put(id, instance);

        // Create and inject ActorContext
        Logger logger = new SimpleLogger(id);
        DefaultActorContext context = new DefaultActorContext(ref, logger, mailbox, actor);
        context.setActorSystem(this);
        
        // Inject WebSocket sender if available
        if (webSocketSender != null) {
            context.setWebSocketSender(webSocketSender);
        }
        
        injectContext(actor, context);

        // Transition to STARTING state
        instance.setState(ActorLifecycleState.STARTING);
        
        try {
            actor.preStart();
            // Transition to RUNNING state after successful start
            instance.setState(ActorLifecycleState.RUNNING);
            System.out.println("Acteur créé et démarré: " + id + " (type: " + type + ") - State: RUNNING");
        } catch (Exception e) {
            // Transition to FAILED state on startup failure
            instance.setState(ActorLifecycleState.FAILED);
            System.err.println("Erreur lors du démarrage de l'acteur " + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        return ref;
    }
    
    /**
     * Inject ActorContext into actor using reflection if setContext method exists.
     */
    private void injectContext(Actor actor, ActorContext context) {
        try {
            java.lang.reflect.Method setContextMethod = actor.getClass().getMethod("setContext", ActorContext.class);
            setContextMethod.invoke(actor, context);
            System.out.println("ActorContext injected into " + actor.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            // Actor doesn't have setContext method, that's ok
        } catch (Exception e) {
            System.err.println("Failed to inject context: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public ActorRef getActor(String id) {
        ActorInstance instance = actors.get(id);
        return instance != null ? instance.ref : null;
    }

    @Override
    public void stop(String id) {
        ActorInstance instance = actors.get(id);
        if (instance != null) {
            // Transition to STOPPING state
            instance.setState(ActorLifecycleState.STOPPING);
            
            try {
                instance.actor.postStop();
                // Transition to STOPPED state after successful stop
                instance.setState(ActorLifecycleState.STOPPED);
                System.out.println("Acteur arrêté: " + id + " - State: STOPPED");
            } catch (Exception e) {
                System.err.println("Erreur lors de l'arrêt de l'acteur " + id + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Send Terminated messages to all watchers (DeathWatch)
                Set<ActorRef> watchersToNotify = instance.getWatchers();
                if (!watchersToNotify.isEmpty()) {
                    Terminated terminatedMsg = new Terminated(instance.ref);
                    System.out.println("Sending Terminated message to " + watchersToNotify.size() + " watchers");
                    for (ActorRef watcher : watchersToNotify) {
                        try {
                            watcher.tell(terminatedMsg);
                        } catch (Exception e) {
                            System.err.println("Failed to send Terminated to watcher " + watcher.getActorId() + ": " + e.getMessage());
                        }
                    }
                }
                
                // Remove from registry
                actors.remove(id);
            }
        }
    }
    
    void blockActor(String id) {
        ActorInstance instance = actors.get(id);
        if (instance != null) {
            ActorLifecycleState currentState = instance.getState();
            if (currentState == ActorLifecycleState.RUNNING) {
                instance.setState(ActorLifecycleState.BLOCKED);
                System.out.println("Acteur bloqué: " + id + " - State: BLOCKED");
            } else {
                System.out.println("Cannot block actor " + id + " in state: " + currentState);
            }
        }
    }
    
    void unblockActor(String id) {
        ActorInstance instance = actors.get(id);
        if (instance != null) {
            ActorLifecycleState currentState = instance.getState();
            if (currentState == ActorLifecycleState.BLOCKED) {
                instance.setState(ActorLifecycleState.RUNNING);
                System.out.println("Acteur débloqué: " + id + " - State: RUNNING");
            } else {
                System.out.println("Cannot unblock actor " + id + " in state: " + currentState);
            }
        }
    }
    
    void restartActor(String id, Throwable cause) {
        ActorInstance instance = actors.get(id);
        if (instance != null) {
            // Transition to RESTARTING state
            instance.setState(ActorLifecycleState.RESTARTING);
            System.out.println("Redémarrage de l'acteur: " + id + " - Cause: " + cause.getMessage());
            
            try {
                // Call preRestart
                instance.actor.preRestart(cause, null);
                
                // Call postRestart
                instance.actor.postRestart(cause);
                
                // Transition back to RUNNING state
                instance.setState(ActorLifecycleState.RUNNING);
                System.out.println("Acteur redémarré: " + id + " - State: RUNNING");
            } catch (Exception e) {
                // If restart fails, mark as FAILED
                instance.setState(ActorLifecycleState.FAILED);
                System.err.println("Erreur lors du redémarrage de l'acteur " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    ActorLifecycleState getActorState(String id) {
        ActorInstance instance = actors.get(id);
        return instance != null ? instance.getState() : null;
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
    
    @Override
    public ActorHealthStatus getActorHealth(String id) {
        ActorInstance instance = actors.get(id);
        
        if (instance == null) {
            return ActorHealthStatus.notFound(id);
        }
        
        ActorLifecycleState state = instance.getState();
        int queueSize = mailbox.size();
        long lastProcessedAt = System.currentTimeMillis(); // TODO: track actual last message time
        
        // Actor is healthy if it's in RUNNING state
        boolean healthy = (state == ActorLifecycleState.RUNNING);
        
        if (healthy) {
            return ActorHealthStatus.healthy(id, state, lastProcessedAt, queueSize);
        } else {
            String errorMsg = "Actor in state: " + state;
            return ActorHealthStatus.unhealthy(id, state, errorMsg);
        }
    }
    
    @Override
    public boolean restartActor(String id) {
        ActorInstance instance = actors.get(id);
        if (instance == null) {
            System.err.println("Cannot restart unknown actor: " + id);
            return false;
        }
        
        try {
            restartActor(id, new RuntimeException("Manual restart requested"));
            return true;
        } catch (Exception e) {
            System.err.println("Failed to restart actor " + id + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<String> getAllActorIds() {
        return new ArrayList<>(actors.keySet());
    }

    void processMessage(String actorId, Message message, ActorRef sender, CompletableFuture<Object> responseFuture) {
        ActorInstance instance = actors.get(actorId);
        if (instance == null) {
            if (responseFuture != null) {
                responseFuture.completeExceptionally(new IllegalStateException("Acteur introuvable: " + actorId));
            }
            return;
        }

        // Check actor state
        ActorLifecycleState state = instance.getState();
        
        // Always enqueue message to mailbox
        mailbox.enqueue(message);
        System.out.println("[MAILBOX] Message ajouté pour acteur " + actorId + " (State: " + state + "): " + message);

        // Only process messages if actor is RUNNING
        if (state == ActorLifecycleState.RUNNING) {
            // Dispatcher : traitement direct pour l'instant
            System.out.println("[DISPATCHER] Traitement du message pour acteur " + actorId);
            Message next = mailbox.dequeue();
            if (next == null) {
                return;
            }

            try {
                instance.actor.receive(next);
            } catch (Exception e) {
                // Apply supervision strategy
                System.err.println("[DISPATCHER] Erreur lors du traitement pour acteur " + actorId + ": " + e.getMessage());
                e.printStackTrace();
                
                SupervisorStrategy strategy = instance.supervisorStrategy;
                SupervisorStrategy.Directive directive = strategy.decide(instance.ref, e, next);
                
                System.out.println("[SUPERVISION] Applying directive " + directive + " for actor " + actorId);
                
                switch (directive) {
                    case RESTART:
                        System.out.println("[SUPERVISION] Restarting actor " + actorId);
                        restartActor(actorId, e);
                        break;
                    case RESUME:
                        System.out.println("[SUPERVISION] Resuming actor " + actorId + " - discarding failed message");
                        // Actor stays in RUNNING state, just continue with next message
                        break;
                    case STOP:
                        System.out.println("[SUPERVISION] Stopping actor " + actorId);
                        stop(actorId);
                        break;
                    case ESCALATE:
                        // For now, just log - hierarchical supervision not yet implemented
                        System.err.println("[SUPERVISION] Escalation not yet implemented for actor " + actorId + " - stopping actor");
                        instance.setState(ActorLifecycleState.FAILED);
                        stop(actorId);
                        break;
                }
                
                if (responseFuture != null) {
                    responseFuture.completeExceptionally(e);
                }
            }
        } else if (state == ActorLifecycleState.BLOCKED) {
            System.out.println("[DISPATCHER] Acteur " + actorId + " est BLOQUÉ - Message en attente");
            // Message stays in mailbox until actor is unblocked
        } else {
            System.out.println("[DISPATCHER] Acteur " + actorId + " dans état " + state + " - Message ignoré ou en attente");
            if (responseFuture != null) {
                responseFuture.completeExceptionally(new IllegalStateException("Actor in state: " + state));
            }
        }
    }

    private static class ActorInstance {
        final Actor actor;
        final ActorRefImpl ref;
        volatile ActorLifecycleState state;
        final Set<ActorRef> watchers = ConcurrentHashMap.newKeySet();
        SupervisorStrategy supervisorStrategy = new OneForOneStrategy();

        ActorInstance(Actor actor, ActorRefImpl ref) {
            this.actor = actor;
            this.ref = ref;
            this.state = ActorLifecycleState.CREATED;
        }
        
        synchronized void setState(ActorLifecycleState newState) {
            this.state = newState;
        }
        
        synchronized ActorLifecycleState getState() {
            return this.state;
        }
        
        void addWatcher(ActorRef watcher) {
            watchers.add(watcher);
        }
        
        void removeWatcher(ActorRef watcher) {
            watchers.remove(watcher);
        }
        
        Set<ActorRef> getWatchers() {
            return new HashSet<>(watchers);
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
        
        @Override
        public void block() {
            system.blockActor(id);
        }
        
        @Override
        public void unblock() {
            system.unblockActor(id);
        }
        
        @Override
        public void restart(Throwable cause) {
            system.restartActor(id, cause);
        }
        
        @Override
        public ActorLifecycleState getState() {
            return system.getActorState(id);
        }
        
        @Override
        public void watch(ActorRef watcher) {
            ActorInstance instance = system.actors.get(id);
            if (instance != null) {
                instance.addWatcher(watcher);
                System.out.println("Actor " + watcher.getActorId() + " is now watching " + id);
            }
        }
        
        @Override
        public void unwatch(ActorRef watcher) {
            ActorInstance instance = system.actors.get(id);
            if (instance != null) {
                instance.removeWatcher(watcher);
                System.out.println("Actor " + watcher.getActorId() + " stopped watching " + id);
            }
        }
    }
}
