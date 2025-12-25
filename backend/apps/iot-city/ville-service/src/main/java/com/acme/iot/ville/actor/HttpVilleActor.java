package com.acme.iot.ville.actor;

import com.acme.iot.city.actors.VilleActor;
import com.acme.saf.actor.core.ActorContext;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Message;
import com.acme.saf.actor.core.SimpleMessage;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * HttpVilleActor - Wrapper extending VilleActor with HTTP communication capabilities.
 * 
 * In microservice architecture, each actor type is deployed in its own service.
 * This class extends the domain VilleActor and runs in the ville-service.
 * It adds the ability to send messages to remote actors via saf-control.
 * 
 * Communication flow:
 * 1. Frontend/client → saf-control
 * 2. saf-control → ville-service (creates HttpVilleActor via /runtime/create-actor)
 * 3. ville-service receives RegisterClient from client-service
 * 4. ville-service receives sensor data from capteur-service
 * 5. ville-service → client-service (ClimateReport via /runtime/tell through saf-control)
 */
public class HttpVilleActor extends VilleActor {
    
    private WebClient webClient;
    private String safControlUrl;
    
    /**
     * Constructor matching VilleActor signature.
     * 
     * @param params Actor parameters including villeName
     */
    public HttpVilleActor(Map<String, Object> params) {
        super(params);
        // SAF Control URL from environment or default
        this.safControlUrl = System.getenv().getOrDefault("SAF_CONTROL_URL", "http://saf-control:8080");
        this.webClient = WebClient.builder()
                .baseUrl(safControlUrl)
                .build();
    }
    
    @Override
    public void setContext(ActorContext context) {
        super.setContext(context);
        // Inject remote messaging capability
        context.setRemoteTransport(new com.acme.saf.actor.core.RemoteMessageTransport() {
            @Override
            public void sendMessage(String actorUrl, com.acme.saf.actor.core.Message message, com.acme.saf.actor.core.ActorRef sender) throws Exception {
                sendRemoteMessage(actorUrl, message, sender);
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Object> askMessage(String actorUrl, com.acme.saf.actor.core.Message message, long timeout, java.util.concurrent.TimeUnit unit) throws Exception {
                // Not implemented for now
                throw new UnsupportedOperationException("Ask pattern not supported for remote actors yet");
            }
            
            @Override
            public boolean checkActorExists(String actorUrl) throws Exception {
                // Not implemented for now
                return false;
            }
            
            @Override
            public void stopActor(String actorUrl) throws Exception {
                // Not implemented for now
                throw new UnsupportedOperationException("Cannot stop remote actors directly");
            }
            
            @Override
            public com.acme.saf.actor.core.ActorLifecycleState getActorState(String actorUrl) throws Exception {
                // Not implemented for now
                return com.acme.saf.actor.core.ActorLifecycleState.STOPPED;
            }
        });
    }
    
    /**
     * Send a message to a remote actor via saf-control.
     */
    private void sendRemoteMessage(String targetActorId, Message message, ActorRef sender) {
        try {
            // Wrap the application-specific message in a generic SimpleMessage
            SimpleMessage wrappedMessage = new SimpleMessage(message);
            
            // Build TellActorCommand with the generic SimpleMessage
            com.acme.saf.actor.core.protocol.TellActorCommand command = 
                new com.acme.saf.actor.core.protocol.TellActorCommand(
                    targetActorId,
                    sender != null ? sender.getActorId() : "",
                    wrappedMessage
                );

            // Send via HTTP to saf-control
            webClient.post()
                    .uri("/api/v1/actors/{actorId}/tell", targetActorId)
                    .header("X-API-KEY", "test")
                    .bodyValue(command)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            result -> System.out.println("Remote message sent to " + targetActorId),
                            error -> System.err.println("Failed to send remote message: " + error.getMessage())
                    );
            
            System.out.println("HttpVilleActor sent remote message to: " + targetActorId);
        } catch (Exception e) {
            System.err.println("Error sending remote message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
