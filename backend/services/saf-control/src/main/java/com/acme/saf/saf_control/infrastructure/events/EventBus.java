package com.acme.saf.saf_control.infrastructure.events;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class EventBus {
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EventBus() {
        // Heartbeat every 15s so browsers/proxies keep the stream alive
        scheduler.scheduleAtFixedRate(() ->
                publish("heartbeat", Map.of("ts", Instant.now().toString())), 0, 15, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        // 0L = no timeout; use a finite value if you want periodic re-connects
        var emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            emitter.send(SseEmitter.event().name("init").data(Map.of("status","ok")));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void publish(String event, Object data) {
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
                emitter.completeWithError(e);
            }
        }
    }
}
