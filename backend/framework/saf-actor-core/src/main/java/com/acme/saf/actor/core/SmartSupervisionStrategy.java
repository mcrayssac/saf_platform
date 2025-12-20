package com.acme.saf.actor.core;


class SmartSupervisionStrategy implements SupervisionStrategy {
    private final ActorContext context;
    
    public SmartSupervisionStrategy(ActorContext context) {
        this.context = context;
    }
    
    @Override
    public SupervisionDirective handleFailure(Actor actor, Throwable cause, Message message) {
        context.logError("Actor failed while processing message: " + message, cause);
        
        // Décision basée sur le type d'exception
        if (cause instanceof IllegalArgumentException) {
            // Mauvais message, on ignore et continue
            context.logWarning("Invalid message received, resuming");
            return SupervisionDirective.RESUME;
            
        } else if (cause instanceof IllegalStateException) {
            // État corrompu, on redémarre
            context.logWarning("Actor state corrupted, restarting");
            return SupervisionDirective.RESTART;
            
        } else if (cause instanceof OutOfMemoryError) {
            // Erreur critique, on escalade au parent
            context.logError("Critical error, escalating to parent", cause);
            return SupervisionDirective.ESCALATE;
            
        } else {
            // Par défaut, on redémarre
            return SupervisionDirective.RESTART;
        }
    }
}