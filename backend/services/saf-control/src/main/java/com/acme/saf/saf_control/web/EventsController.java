package com.acme.saf.saf_control.web;

import com.acme.saf.saf_control.infrastructure.events.EventBus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Events")
public class EventsController {
    private final EventBus events;
    public EventsController(EventBus events) { this.events = events; }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream of platform events (mock)")
    public SseEmitter stream() {
        return events.subscribe();
    }


}
