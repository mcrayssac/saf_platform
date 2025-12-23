package com.acme.saf.actor.core;

/**
 * System message sent to watchers when an actor terminates.
 * This is part of the DeathWatch pattern that allows actors to monitor
 * the lifecycle of other actors.
 * 
 * When an actor terminates (either normally or due to failure), all actors
 * that are watching it will receive a Terminated message.
 * 
 * Example usage:
 * <pre>
 * public class ParentActor implements Actor {
 *     private ActorRef child;
 *     
 *     public void receive(Message message) {
 *         if (message instanceof Terminated) {
 *             Terminated terminated = (Terminated) message;
 *             if (terminated.getActor().equals(child)) {
 *                 // Child actor has terminated
 *                 // Take appropriate action (restart, create new child, etc.)
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public class Terminated implements Message {
    
    private final ActorRef actor;
    private final boolean existenceConfirmed;
    private final boolean addressTerminated;
    
    /**
     * Creates a Terminated message for an actor that has stopped.
     * 
     * @param actor the actor that has terminated
     */
    public Terminated(ActorRef actor) {
        this(actor, true, true);
    }
    
    /**
     * Creates a Terminated message with detailed information.
     * 
     * @param actor the actor that has terminated
     * @param existenceConfirmed whether the actor's existence was confirmed before termination
     * @param addressTerminated whether the actor's address is now invalid
     */
    public Terminated(ActorRef actor, boolean existenceConfirmed, boolean addressTerminated) {
        this.actor = actor;
        this.existenceConfirmed = existenceConfirmed;
        this.addressTerminated = addressTerminated;
    }
    
    /**
     * Gets the actor that has terminated.
     * 
     * @return the terminated actor reference
     */
    public ActorRef getActor() {
        return actor;
    }
    
    @Override
    public Object getPayload() {
        return actor;
    }
    
    /**
     * Checks if the actor's existence was confirmed before termination.
     * 
     * @return true if existence was confirmed
     */
    public boolean isExistenceConfirmed() {
        return existenceConfirmed;
    }
    
    /**
     * Checks if the actor's address has been terminated.
     * 
     * @return true if the address is now invalid
     */
    public boolean isAddressTerminated() {
        return addressTerminated;
    }
    
    @Override
    public String toString() {
        return "Terminated{" +
                "actor=" + actor.getActorId() +
                ", existenceConfirmed=" + existenceConfirmed +
                ", addressTerminated=" + addressTerminated +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminated that = (Terminated) o;
        return actor.equals(that.actor);
    }
    
    @Override
    public int hashCode() {
        return actor.hashCode();
    }
}
