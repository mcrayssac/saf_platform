package com.acme.iot.runtime.controller;

import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Actor Management in IoT Runtime
 * Provides endpoints to create, manage, and send messages to actors
 */
@RestController
@RequestMapping("/actors")
public class ActorController {

    private final ActorSystem actorSystem;

    @Autowired
    public ActorController(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    /**
     * Create a new actor
     * POST /actors
     * Body: {"type": "CLIENT", "params": {"name": "John", "email": "john@example.com"}}
     */
    @PostMapping
    public ResponseEntity<ActorCreationResponse> createActor(@RequestBody ActorRequest request) {
        try {
            ActorRef actorRef = actorSystem.spawn(request.getType(), request.getParams());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ActorCreationResponse(
                    actorRef.getActorId(),
                    request.getType(),
                    "SUCCESS",
                    "Actor created successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ActorCreationResponse(
                    null,
                    request.getType(),
                    "ERROR",
                    "Failed to create actor: " + e.getMessage()
            ));
        }
    }

    /**
     * Send a message to an actor
     * POST /actors/{actorId}/messages
     * Body: {"type": "COMMAND", "content": {...}}
     */
    @PostMapping("/{actorId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String actorId,
            @RequestBody MessageRequest request) {
        try {
            ActorRef actorRef = actorSystem.getActor(actorId);
            if (actorRef == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(
                        actorId,
                        request.getType(),
                        "ERROR",
                        "Actor not found"
                ));
            }
            
            // Create a simple message implementation
            Message message = new SimpleMessage(request.getType(), request.getContent());
            actorRef.tell(message);
            
            return ResponseEntity.ok(new MessageResponse(
                    actorId,
                    request.getType(),
                    "SUCCESS",
                    "Message sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(
                    actorId,
                    request.getType(),
                    "ERROR",
                    "Failed to send message: " + e.getMessage()
            ));
        }
    }

    /**
     * Get actor information
     * GET /actors/{actorId}
     */
    @GetMapping("/{actorId}")
    public ResponseEntity<ActorInfoResponse> getActorInfo(@PathVariable String actorId) {
        try {
            // TODO: Implement actual actor info retrieval from ActorSystem
            return ResponseEntity.ok(new ActorInfoResponse(
                    actorId,
                    "UNKNOWN",
                    "RUNNING",
                    "Actor info endpoint - implementation pending"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ActorInfoResponse(
                    actorId,
                    "UNKNOWN",
                    "ERROR",
                    "Actor not found: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete/Stop an actor
     * DELETE /actors/{actorId}
     */
    @DeleteMapping("/{actorId}")
    public ResponseEntity<MessageResponse> deleteActor(@PathVariable String actorId) {
        try {
            actorSystem.stop(actorId);
            return ResponseEntity.ok(new MessageResponse(
                    actorId,
                    "DELETE",
                    "SUCCESS",
                    "Actor stopped successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(
                    actorId,
                    "DELETE",
                    "ERROR",
                    "Failed to stop actor: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /actors/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "IoT-Runtime",
                "actorSystem", actorSystem != null ? "ACTIVE" : "INACTIVE"
        ));
    }

    // ===== Request/Response DTOs =====

    public static class ActorRequest {
        private String type;
        private Map<String, Object> params;

        public ActorRequest() {}

        public ActorRequest(String type, Map<String, Object> params) {
            this.type = type;
            this.params = params;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }
    }

    public static class ActorCreationResponse {
        private String actorId;
        private String type;
        private String status;
        private String message;

        public ActorCreationResponse(String actorId, String type, String status, String message) {
            this.actorId = actorId;
            this.type = type;
            this.status = status;
            this.message = message;
        }

        public String getActorId() {
            return actorId;
        }

        public String getType() {
            return type;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class MessageRequest {
        private String type;
        private Map<String, Object> content;

        public MessageRequest() {}

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getContent() {
            return content;
        }

        public void setContent(Map<String, Object> content) {
            this.content = content;
        }
    }

    public static class MessageResponse {
        private String actorId;
        private String messageType;
        private String status;
        private String message;

        public MessageResponse(String actorId, String messageType, String status, String message) {
            this.actorId = actorId;
            this.messageType = messageType;
            this.status = status;
            this.message = message;
        }

        public String getActorId() {
            return actorId;
        }

        public String getMessageType() {
            return messageType;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ActorInfoResponse {
        private String actorId;
        private String type;
        private String status;
        private String message;

        public ActorInfoResponse(String actorId, String type, String status, String message) {
            this.actorId = actorId;
            this.type = type;
            this.status = status;
            this.message = message;
        }

        public String getActorId() {
            return actorId;
        }

        public String getType() {
            return type;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Simple Message implementation for REST API
     */
    private static class SimpleMessage implements Message {
        private final String type;
        private final Object payload;

        public SimpleMessage(String type, Object payload) {
            this.type = type;
            this.payload = payload;
        }

        @Override
        public Object getPayload() {
            return Map.of("type", type, "content", payload);
        }
    }
}
