package com.acme.saf.actor.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultActorContextTest {

    @Mock private Logger mockLogger;
    @Mock private ActorRef mockSelf;
    @Mock private ActorRef mockSender;
    @Mock private Actor mockActor;
    @Mock private Message mockMessage;
    @Mock private Mailbox mockMailbox;

    private DefaultActorContext context;

    @BeforeEach
    void setUp() {
        context = new DefaultActorContext(mockSelf, mockLogger, mockMailbox, mockActor);
    }

    @Test
    @DisplayName("Invariant: getters must return injected instances")
    void testGetters() {
        assertEquals(mockMailbox, context.getMailbox());
        assertEquals(mockActor, context.getActor());
        assertEquals(mockSelf, context.self());
    }

    @Test
    @DisplayName("Invariant: sender() update")
    void testSenderUpdate() {
        assertNull(context.sender());
        context.setSender(mockSender);
        assertEquals(mockSender, context.sender());
    }

    @Test
    @DisplayName("Logging delegation")
    void testLogging() {
        context.logInfo("Test");
        verify(mockLogger).info("Test");
    }

    @Test
    @DisplayName("WebSocket Check (Default is false)")
    void testWebSocketDefaults() {
        assertFalse(context.hasWebSocketConnection());
    }
    
    @Test
    @DisplayName("INTEGRATION: Supervision")
    void testSupervisionIntegration() {
        SupervisionStrategy strategy = new SmartSupervisionStrategy(context);
        Exception crash = new IllegalStateException("Corrupted");
        
        // Depending on how SmartSupervisionStrategy is implemented by your team, 
        // it might use logWarning or logError. We verify usage of the context.
        SupervisionDirective directive = strategy.handleFailure(mockActor, crash, mockMessage);
        
        verify(mockLogger).warning(contains("Actor state corrupted"));
        assertEquals(SupervisionDirective.RESTART, directive);
    }
}