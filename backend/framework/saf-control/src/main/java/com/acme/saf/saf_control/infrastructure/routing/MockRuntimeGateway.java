package com.acme.saf.saf_control.infrastructure.routing;

import com.acme.saf.saf_control.infrastructure.events.EventBus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockRuntimeGateway implements RuntimeGateway {

    private final EventBus events;

    public MockRuntimeGateway(EventBus events) {
        this.events = events;
    }

    @Override
    public void dispatch(RuntimeMessageEnvelope envelope) {
        // Simule l’envoi au Runtime.
        // Pour le moment : on pousse un event SSE pour visualiser ce qui partirait.
        Map<String, Object> event = Map.of(
                "agentId", envelope.agentId(),
                "node", envelope.node(),
                "mode", envelope.mode(),
                "payload", envelope.payload(),
                "timeoutMs", envelope.timeoutMs(),
                "correlationId", envelope.correlationId()
        );

        events.publish("RuntimeMessageDispatched", event);
    }
}
