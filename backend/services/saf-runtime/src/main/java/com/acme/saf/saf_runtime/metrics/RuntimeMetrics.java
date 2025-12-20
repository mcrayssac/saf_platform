package main.java.com.acme.saf.saf_runtime.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RuntimeMetrics {
    public RuntimeMetrics(MeterRegistry registry, RuntimeMetricsStore store) {
        Gauge.builder("saf.runtime.actors.count", store, RuntimeMetricsStore::actorCount)
                .description("Number of actors running in the runtime")
                .register(registry);
        Gauge.builder("saf.runtime.mailbox.avg.size", store, RuntimeMetricsStore::averageMailboxSize)
                .description("Average mailbox size across runtime actors")
                .register(registry);
    }
}