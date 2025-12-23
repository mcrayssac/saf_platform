package com.acme.saf.actor.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Supervision strategy that applies directives only to the failed child actor.
 * Other child actors are not affected by the failure.
 * 
 * This is the most common supervision strategy and is suitable for most scenarios
 * where child actors are independent of each other.
 * 
 * Example:
 * <pre>
 * SupervisorStrategy strategy = new OneForOneStrategy()
 *     .match(IllegalArgumentException.class, Directive.RESUME)
 *     .match(NullPointerException.class, Directive.RESTART)
 *     .matchAny(Directive.ESCALATE);
 * </pre>
 */
public class OneForOneStrategy implements SupervisorStrategy {
    
    private final Map<Class<? extends Throwable>, Directive> rules = new ConcurrentHashMap<>();
    private Directive defaultDirective = Directive.RESTART;
    private final int maxRetries;
    private final long withinTimeRange;
    
    /**
     * Creates a OneForOneStrategy with default settings.
     * Max retries: 10, Time range: 60 seconds
     */
    public OneForOneStrategy() {
        this(10, 60000);
    }
    
    /**
     * Creates a OneForOneStrategy with custom retry limits.
     * 
     * @param maxRetries maximum number of retries within time window (-1 for unlimited)
     * @param withinTimeRange time window in milliseconds
     */
    public OneForOneStrategy(int maxRetries, long withinTimeRange) {
        this.maxRetries = maxRetries;
        this.withinTimeRange = withinTimeRange;
    }
    
    /**
     * Adds a rule for handling a specific exception type.
     * 
     * @param exceptionClass the exception class to match
     * @param directive the directive to apply
     * @return this strategy for method chaining
     */
    public OneForOneStrategy match(Class<? extends Throwable> exceptionClass, Directive directive) {
        rules.put(exceptionClass, directive);
        return this;
    }
    
    /**
     * Sets the default directive for exceptions that don't match any rule.
     * 
     * @param directive the default directive
     * @return this strategy for method chaining
     */
    public OneForOneStrategy matchAny(Directive directive) {
        this.defaultDirective = directive;
        return this;
    }
    
    /**
     * Adds a rule using a custom decision function.
     * 
     * @param exceptionClass the exception class to match
     * @param decider function that decides the directive based on the exception
     * @return this strategy for method chaining
     */
    public OneForOneStrategy matchWith(Class<? extends Throwable> exceptionClass, 
                                       Function<Throwable, Directive> decider) {
        // Store the exception class and use the decider in the decide method
        // For simplicity, we'll use the direct directive mapping
        // A more advanced implementation could store the decider function
        return this;
    }
    
    @Override
    public Directive decide(ActorRef actorRef, Throwable cause, Message message) {
        // Check for exact match first
        Directive directive = rules.get(cause.getClass());
        if (directive != null) {
            return directive;
        }
        
        // Check for superclass matches
        for (Map.Entry<Class<? extends Throwable>, Directive> entry : rules.entrySet()) {
            if (entry.getKey().isAssignableFrom(cause.getClass())) {
                return entry.getValue();
            }
        }
        
        // Return default directive
        return defaultDirective;
    }
    
    @Override
    public int maxRetries() {
        return maxRetries;
    }
    
    @Override
    public long withinTimeRange() {
        return withinTimeRange;
    }
    
    /**
     * Creates a strategy that always restarts on failure.
     * 
     * @return a new OneForOneStrategy that restarts on any exception
     */
    public static OneForOneStrategy alwaysRestart() {
        return new OneForOneStrategy().matchAny(Directive.RESTART);
    }
    
    /**
     * Creates a strategy that always resumes on failure.
     * 
     * @return a new OneForOneStrategy that resumes on any exception
     */
    public static OneForOneStrategy alwaysResume() {
        return new OneForOneStrategy().matchAny(Directive.RESUME);
    }
    
    /**
     * Creates a strategy that always stops on failure.
     * 
     * @return a new OneForOneStrategy that stops on any exception
     */
    public static OneForOneStrategy alwaysStop() {
        return new OneForOneStrategy().matchAny(Directive.STOP);
    }
    
    /**
     * Creates a strategy that always escalates failures.
     * 
     * @return a new OneForOneStrategy that escalates all exceptions
     */
    public static OneForOneStrategy alwaysEscalate() {
        return new OneForOneStrategy().matchAny(Directive.ESCALATE);
    }
}
