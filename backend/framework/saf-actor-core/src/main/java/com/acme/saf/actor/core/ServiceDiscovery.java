package com.acme.saf.actor.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServiceDiscovery provides a simple registry for locating remote actor services.
 * Maps service names to their base URLs for cross-service communication.
 * 
 * This is a simple in-memory implementation. In production, this could be replaced
 * with external service discovery like Consul, Eureka, or Kubernetes service discovery.
 * 
 * Features:
 * - Service registration and lookup
 * - Thread-safe concurrent access
 * - Simple API for RemoteActorRef creation
 * 
 * Example:
 * <pre>
 * // Register services
 * ServiceDiscovery.register("iot-runtime", "http://iot-runtime:8081");
 * ServiceDiscovery.register("payment-service", "http://payment:8082");
 * 
 * // Lookup service URL
 * String url = ServiceDiscovery.getServiceUrl("iot-runtime");
 * 
 * // Create remote actor reference
 * RemoteActorRef remote = ServiceDiscovery.createRemoteRef(
 *     "iot-runtime", 
 *     "client-123",
 *     transport
 * );
 * </pre>
 */
public class ServiceDiscovery {
    
    private static final Map<String, String> serviceRegistry = new ConcurrentHashMap<>();
    
    /**
     * Registers a service with its base URL.
     * 
     * @param serviceName the service name (e.g., "iot-runtime")
     * @param baseUrl the base URL (e.g., "http://iot-runtime:8081")
     */
    public static void register(String serviceName, String baseUrl) {
        if (serviceName == null || serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        
        // Normalize URL (remove trailing slash)
        String normalizedUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        serviceRegistry.put(serviceName, normalizedUrl);
        System.out.println("ServiceDiscovery: Registered " + serviceName + " → " + normalizedUrl);
    }
    
    /**
     * Looks up the base URL for a service.
     * 
     * @param serviceName the service name
     * @return the base URL, or null if not found
     */
    public static String getServiceUrl(String serviceName) {
        return serviceRegistry.get(serviceName);
    }
    
    /**
     * Checks if a service is registered.
     * 
     * @param serviceName the service name
     * @return true if the service is registered
     */
    public static boolean isRegistered(String serviceName) {
        return serviceRegistry.containsKey(serviceName);
    }
    
    /**
     * Unregisters a service.
     * 
     * @param serviceName the service name
     * @return true if the service was registered and removed
     */
    public static boolean unregister(String serviceName) {
        boolean removed = serviceRegistry.remove(serviceName) != null;
        if (removed) {
            System.out.println("ServiceDiscovery: Unregistered " + serviceName);
        }
        return removed;
    }
    
    /**
     * Gets all registered services.
     * 
     * @return a map of service names to URLs
     */
    public static Map<String, String> getAllServices() {
        return new ConcurrentHashMap<>(serviceRegistry);
    }
    
    /**
     * Clears all registered services.
     * Useful for testing.
     */
    public static void clear() {
        serviceRegistry.clear();
        System.out.println("ServiceDiscovery: Cleared all registrations");
    }
    
    /**
     * Creates a RemoteActorRef for an actor in a registered service.
     * 
     * @param serviceName the service name (must be registered)
     * @param actorId the actor ID in that service
     * @param transport the message transport implementation
     * @return a RemoteActorRef for the actor
     * @throws IllegalArgumentException if service is not registered
     */
    public static RemoteActorRef createRemoteRef(String serviceName, String actorId, RemoteMessageTransport transport) {
        String serviceUrl = getServiceUrl(serviceName);
        if (serviceUrl == null) {
            throw new IllegalArgumentException("Service not registered: " + serviceName + 
                ". Available services: " + serviceRegistry.keySet());
        }
        
        return new RemoteActorRef.Builder()
            .withActorId(actorId)
            .withServiceName(serviceName)
            .withServiceUrl(serviceUrl)
            .withTransport(transport)
            .build();
    }
    
    /**
     * Creates a RemoteActorRef using a full actor path.
     * Path format: actorId@serviceName
     * 
     * @param actorPath the actor path (e.g., "client-123@iot-runtime")
     * @param transport the message transport implementation
     * @return a RemoteActorRef for the actor
     * @throws IllegalArgumentException if path is invalid or service not registered
     */
    public static RemoteActorRef createRemoteRefFromPath(String actorPath, RemoteMessageTransport transport) {
        if (actorPath == null || !actorPath.contains("@")) {
            throw new IllegalArgumentException("Invalid actor path format. Expected: actorId@serviceName");
        }
        
        String[] parts = actorPath.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid actor path format. Expected: actorId@serviceName");
        }
        
        String actorId = parts[0].trim();
        String serviceName = parts[1].trim();
        
        return createRemoteRef(serviceName, actorId, transport);
    }
    
    @Override
    public String toString() {
        return "ServiceDiscovery{" +
                "registeredServices=" + serviceRegistry.size() +
                ", services=" + serviceRegistry.keySet() +
                '}';
    }
}
