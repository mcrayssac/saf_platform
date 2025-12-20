package com.acme.saf.actor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.contains;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultActorContextTest {

    // On mock les dépendances
    @Mock
    private Logger mockLogger;
    @Mock
    private ActorRef mockSelf;
    @Mock
    private ActorRef mockSender;
    @Mock
    private Actor mockActor;
    @Mock
    private Message mockMessage;

    private DefaultActorContext context;

    @BeforeEach
    void setUp() {
        // Initalisation avant chaque test
        context = new DefaultActorContext(mockSelf, mockLogger);
    }

    @Test
    @DisplayName("Invariant: self() doit retourner la référence fournie au constructeur")
    void testSelfReferenceInvariant() {
        assertEquals(mockSelf, context.self(), "Le contexte doit retourner le bon ActorRef (self)");
    }

    @Test
    @DisplayName("Invariant: sender() doit retourner l'acteur qui a envoyé le message")
    void testSenderUpdate() {
        // Au début sender peut être null
        assertNull(context.sender());

        // Simulation: Le Runtime injecte le sender avant le traitement d'un message
        context.setSender(mockSender);

        assertEquals(mockSender, context.sender(), "Le contexte doit mettre à jour le sender");
    }

    @Test
    @DisplayName("Logging: logInfo doit déléguer au Logger sous-jacent")
    void testLogInfoDelegation() {
        String testMsg = "Actor started";
        
        //Action
        context.logInfo(testMsg);

        // Vérification que la méthode info du mockLogger a bien été appelée
        verify(mockLogger, times(1)).info(testMsg);
    }

    @Test
    @DisplayName("Logging: logError doit passer l'exception au Logger")
    void testLogErrorDelegation() {
        String errorMsg = "Erreur";
        RuntimeException ex = new RuntimeException("Crash");

        // Action
        context.logError(errorMsg, ex);

        // Vérification
        verify(mockLogger).error(errorMsg, ex);
    }

    @Test
    @DisplayName("INTEGRATION: La stratégie de supervision doit pouvoir utiliser le contexte")
    void testSupervisionIntegration() {

        SupervisionStrategy strategy = new SmartSupervisionStrategy(context);

        // On simule un crash
        Exception crash = new IllegalStateException("Corrupted");

        // La stratégie décide quoi faire
        SupervisionDirective directive = strategy.handleFailure(mockActor, crash, mockMessage);

        // On vérifie que la stratégie a bien utilisé le logger pour signaler le problème
        verify(mockLogger).warning(contains("Actor state corrupted"));
        
        // On vérifie que la décision est bien RESTART
        assertEquals(SupervisionDirective.RESTART, directive);
    }
    
    @Test
    @DisplayName("Le contexte doit publier les événements")
    void testEventPublishing() {
        var event = new ActorLifecycleEvent.ActorStarted("id-1", "path/to/actor");
        
        context.publishEvent(event);
        
        verify(mockLogger).info(contains("Event:"));
    }
}