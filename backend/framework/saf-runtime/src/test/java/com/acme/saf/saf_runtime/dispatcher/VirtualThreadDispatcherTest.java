package com.acme.saf.saf_runtime.dispatcher;

import com.acme.saf.actor.core.*;
import com.acme.saf.saf_runtime.InMemoryMailbox;

import com.acme.saf.saf_runtime.dispatcher.VirtualThreadDispatcher;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class VirtualThreadDispatcherTest {

    @Test
    void shouldDispatchAndProcessMessage() {
        VirtualThreadDispatcher dispatcher = new VirtualThreadDispatcher();
        
        // Mocks
        Actor mockActor = mock(Actor.class);
        ActorRef mockRef = mock(ActorRef.class);
        when(mockRef.getActorId()).thenReturn("test-actor-1");
        
        SupervisionStrategy mockStrategy = mock(SupervisionStrategy.class);

        // Vraie Mailbox avec un message
        Mailbox realMailbox = new InMemoryMailbox();
        Message msg = new SimpleMessage("Coucou !");
        realMailbox.enqueue(msg);

        // On appelle dispatch avec la nouvelle signature
        dispatcher.dispatch(mockRef, realMailbox, mockActor, mockStrategy);

        // Assertion
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            // L'acteur doit avoir reçu le message
            verify(mockActor, times(1)).receive(msg);
            
            // La mailbox doit être vide
            assert(realMailbox.isEmpty());
        });
    }

    // Stub Message pour le test
    static class SimpleMessage implements Message {
        private final Object payload;
        public SimpleMessage(Object p) { this.payload = p; }
        public Object getPayload() { return payload; }
        public Object payload() { return payload; }
        public String type() { return "Simple"; }
        public String getType() { return "Simple"; }
    }
}