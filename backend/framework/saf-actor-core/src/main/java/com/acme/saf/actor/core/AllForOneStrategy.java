package com.acme.saf.actor.core;

/**
 * Supervision strategy that applies directives to all child actors when one fails.
 * When a child actor fails, all its siblings are affected by the supervision decision.
 * 
 * This strategy is useful when child actors are interdependent and their states
 * must remain consistent. For example, if actors share state or depend on each other's results.
 * 
 * Example:
 * <pre>
 * SupervisorStrategy strategy = new AllForOneStrategy()
 *     .match(DatabaseException.class, Directive.RESTART)
 *     .matchAny(Directive.STOP);
 * // When one actor fails with DatabaseException, ALL children are restarted
 * </pre>
 */
public class AllForOneStrategy extends OneForOneStrategy {
    
    /**
     * Creates an AllForOneStrategy with default settings.
     * Max retries: 10, Time range: 60 seconds
     */
    public AllForOneStrategy() {
        super();
    }
    
    /**
     * Creates an AllForOneStrategy with custom retry limits.
     * 
     * @param maxRetries maximum number of retries within time window (-1 for unlimited)
     * @param withinTimeRange time window in milliseconds
     */
    public AllForOneStrategy(int maxRetries, long withinTimeRange) {
        super(maxRetries, withinTimeRange);
    }
    
    @Override
    public AllForOneStrategy match(Class<? extends Throwable> exceptionClass, Directive directive) {
        super.match(exceptionClass, directive);
        return this;
    }
    
    @Override
    public AllForOneStrategy matchAny(Directive directive) {
        super.matchAny(directive);
        return this;
    }
    
    /**
     * Indicates that this strategy affects all children.
     * The actor system will apply the directive to all children, not just the failed one.
     * 
     * @return true (always affects all children)
     */
    public boolean affectsAllChildren() {
        return true;
    }
    
    /**
     * Creates a strategy that always restarts all children on any failure.
     * 
     * @return a new AllForOneStrategy that restarts all children on any exception
     */
    public static AllForOneStrategy alwaysRestartAll() {
        return new AllForOneStrategy().matchAny(Directive.RESTART);
    }
    
    /**
     * Creates a strategy that always stops all children on any failure.
     * 
     * @return a new AllForOneStrategy that stops all children on any exception
     */
    public static AllForOneStrategy alwaysStopAll() {
        return new AllForOneStrategy().matchAny(Directive.STOP);
    }
}
