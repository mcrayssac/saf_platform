package com.acme.saf.saf_control.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Transparent proxy controller that forwards all actor operations to the configured runtime.
 * This controller is framework-agnostic and has NO knowledge of actor types or session logic.
 * It simply provides encapsulation, monitoring, and routing capabilities.
 */
@Tag(name = "Actors", description = "Actor lifecycle and messaging operations (proxied to runtime)")
@SecurityRequirement(name = "X-API-KEY")
@RestController
@RequestMapping("/api")
@CrossOrigin(
    origins = "http://localhost",
    allowedHeaders = {"*"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class RuntimeProxyController {
    
    private final WebClient webClient;
    
    public RuntimeProxyController(@Value("${runtime.default.url:http://iot-runtime:8081}") String runtimeUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(runtimeUrl)
                .build();
    }
    
    /**
     * Proxy: Create actor in runtime
     * POST /api/actors
     */
    @Operation(summary = "Create a new actor", 
               description = "Creates a new actor in the runtime. The request is transparently proxied to the configured runtime service.")
    @PostMapping("/actors")
    public Mono<ResponseEntity<Map>> createActor(@RequestBody Map<String, Object> request) {
        return webClient.post()
                .uri("/actors")
                .bodyValue(request)
                .retrieve()
                .toEntity(Map.class);
    }
    
    /**
     * Proxy: Send message to actor
     * POST /api/actors/{actorId}/messages
     */
    @Operation(summary = "Send message to actor",
               description = "Sends a message to a specific actor. The message is transparently proxied to the runtime.")
    @PostMapping("/actors/{actorId}/messages")
    public Mono<ResponseEntity<Map>> sendMessage(
            @Parameter(description = "Unique identifier of the target actor", required = true)
            @PathVariable String actorId,
            @RequestBody Map<String, Object> message) {
        return webClient.post()
                .uri("/actors/" + actorId + "/messages")
                .bodyValue(message)
                .retrieve()
                .toEntity(Map.class);
    }
    
    /**
     * Proxy: Get actor info
     * GET /api/actors/{actorId}
     */
    @Operation(summary = "Get actor information",
               description = "Retrieves information about a specific actor from the runtime.")
    @GetMapping("/actors/{actorId}")
    public Mono<ResponseEntity<Map>> getActor(
            @Parameter(description = "Unique identifier of the actor", required = true)
            @PathVariable String actorId) {
        return webClient.get()
                .uri("/actors/" + actorId)
                .retrieve()
                .toEntity(Map.class);
    }
    
    /**
     * Proxy: Stop/delete actor
     * DELETE /api/actors/{actorId}
     */
    @Operation(summary = "Stop and delete actor",
               description = "Stops and removes an actor from the runtime.")
    @DeleteMapping("/actors/{actorId}")
    public Mono<ResponseEntity<Void>> stopActor(
            @Parameter(description = "Unique identifier of the actor to stop", required = true)
            @PathVariable String actorId) {
        return webClient.delete()
                .uri("/actors/" + actorId)
                .retrieve()
                .toEntity(Void.class);
    }
    
    /**
     * Proxy: Health check
     * GET /api/actors/health
     */
    @Operation(summary = "Health check",
               description = "Checks the health status of the actor runtime.")
    @GetMapping("/actors/health")
    public Mono<ResponseEntity<Map>> health() {
        return webClient.get()
                .uri("/actors/health")
                .retrieve()
                .toEntity(Map.class);
    }
    
    /**
     * Generic wildcard proxy for any runtime-specific API endpoints.
     * This allows application runtimes to expose custom endpoints (e.g., /api/cities, /api/sensors)
     * without requiring changes to the framework's saf-control layer.
     * 
     * Pattern: GET /api/{path}/** -> forwards to runtime: /api/{path}/**
     * 
     * Framework-agnostic - applications can expose any endpoints they need.
     */
    @Operation(summary = "Generic API proxy",
               description = "Transparently proxies any API request to the configured runtime. Allows runtimes to expose custom endpoints without framework changes.")
    @GetMapping("/**")
    public Mono<ResponseEntity<Object>> proxyGetRequest(
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + 4); // Remove "/api"
        return webClient.get()
                .uri("/api" + path + (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                .retrieve()
                .toEntity(Object.class);
    }
    
    /**
     * Generic wildcard proxy for POST requests
     */
    @PostMapping("/**")
    public Mono<ResponseEntity<Object>> proxyPostRequest(
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) Object body) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + 4);
        return webClient.post()
                .uri("/api" + path)
                .bodyValue(body != null ? body : Map.of())
                .retrieve()
                .toEntity(Object.class);
    }
    
    /**
     * Generic wildcard proxy for PUT requests
     */
    @PutMapping("/**")
    public Mono<ResponseEntity<Object>> proxyPutRequest(
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletRequest request,
            @RequestBody(required = false) Object body) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + 4);
        return webClient.put()
                .uri("/api" + path)
                .bodyValue(body != null ? body : Map.of())
                .retrieve()
                .toEntity(Object.class);
    }
    
    /**
     * Generic wildcard proxy for DELETE requests
     */
    @DeleteMapping("/**")
    public Mono<ResponseEntity<Void>> proxyDeleteRequest(
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + 4);
        return webClient.delete()
                .uri("/api" + path)
                .retrieve()
                .toEntity(Void.class);
    }
}
