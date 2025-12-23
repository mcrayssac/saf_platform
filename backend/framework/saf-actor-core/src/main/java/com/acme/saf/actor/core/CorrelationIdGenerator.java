package com.acme.saf.actor.core;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CorrelationIdGenerator creates unique correlation IDs for distributed tracing.
 * These IDs track requests/messages across actors, services, and system boundaries.
 * 
 * Correlation IDs enable:
 * - Tracing message flows through the actor system
 * - Correlating logs across distributed services
 * - Performance analysis of end-to-end operations
 * - Debugging complex inter-actor communication
 * 
 * Formats supported:
 * - UUID (default): Globally unique, standard format
 * - Short: Compact alphanumeric (for readability in logs)
 * - Custom: Application-specific format
 * 
 * Example:
 * <pre>
 * // Generate UUID-based correlation ID
 * String correlationId = CorrelationIdGenerator.generate();
 * // Result: "550e8400-e29b-41d4-a716-446655440000"
 * 
 * // Generate short correlation ID
 * String shortId = CorrelationIdGenerator.generateShort();
 * // Result: "a7f3k9x2"
 * 
 * // Use in logging
 * logger.logMessage(from, to, message, correlationId);
 * </pre>
 */
public class CorrelationIdGenerator {
    
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_ID_LENGTH = 8;
    
    /**
     * Generates a UUID-based correlation ID.
     * This is the default and recommended format for global uniqueness.
     * 
     * @return a UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates a short, compact correlation ID.
     * Useful for logs where readability is important.
     * Note: Not globally unique, but sufficient for most tracing scenarios.
     * 
     * @return an 8-character alphanumeric string (e.g., "a7f3k9x2")
     */
    public static String generateShort() {
        return generateShort(SHORT_ID_LENGTH);
    }
    
    /**
     * Generates a short correlation ID with custom length.
     * 
     * @param length the desired length of the correlation ID
     * @return an alphanumeric string of the specified length
     */
    public static String generateShort(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARS.length());
            sb.append(CHARS.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * Generates a timestamped correlation ID.
     * Includes current timestamp for temporal ordering.
     * 
     * Format: {timestamp}-{uuid}
     * Example: "1703332800000-550e8400-e29b-41d4-a716-446655440000"
     * 
     * @return a timestamped correlation ID
     */
    public static String generateTimestamped() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generates a correlation ID with a custom prefix.
     * Useful for identifying request sources or types.
     * 
     * Format: {prefix}-{uuid}
     * 
     * @param prefix the prefix to add (e.g., "api", "batch", "ui")
     * @return a prefixed correlation ID
     */
    public static String generateWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return generate();
        }
        return prefix + "-" + UUID.randomUUID().toString();
    }
    
    /**
     * Generates a hierarchical correlation ID for nested operations.
     * Creates a child ID that maintains relationship to parent.
     * 
     * Format: {parentId}.{childSequence}
     * 
     * @param parentId the parent correlation ID
     * @param childSequence the child sequence number
     * @return a hierarchical correlation ID
     */
    public static String generateChild(String parentId, int childSequence) {
        if (parentId == null || parentId.isEmpty()) {
            return generate();
        }
        return parentId + "." + childSequence;
    }
    
    /**
     * Validates if a string is a valid UUID format.
     * 
     * @param correlationId the ID to validate
     * @return true if valid UUID format
     */
    public static boolean isValidUUID(String correlationId) {
        if (correlationId == null) {
            return false;
        }
        try {
            UUID.fromString(correlationId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Extracts the prefix from a prefixed correlation ID.
     * Returns null if no prefix exists.
     * 
     * @param correlationId the correlation ID
     * @return the prefix, or null if none
     */
    public static String extractPrefix(String correlationId) {
        if (correlationId == null || !correlationId.contains("-")) {
            return null;
        }
        
        String[] parts = correlationId.split("-", 2);
        // Check if second part looks like a UUID
        if (parts.length == 2 && isValidUUID(parts[1])) {
            return parts[0];
        }
        
        return null;
    }
    
    /**
     * Extracts the parent ID from a hierarchical correlation ID.
     * Returns null if not hierarchical.
     * 
     * @param correlationId the correlation ID
     * @return the parent ID, or null if not hierarchical
     */
    public static String extractParent(String correlationId) {
        if (correlationId == null || !correlationId.contains(".")) {
            return null;
        }
        
        int lastDot = correlationId.lastIndexOf('.');
        return correlationId.substring(0, lastDot);
    }
}
