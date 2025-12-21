package main.java.com.acme.saf.saf_runtime.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RuntimeMetricsStore {
    private final AtomicInteger actorCount = new AtomicInteger(0);
    private final AtomicReference<Double> averageMailboxSize = new AtomicReference<>(0.0);

    public int actorCount() {
        return actorCount.get();
    }

    public void setActorCount(int count) {
        actorCount.set(count);
    }

    public double averageMailboxSize() {
        return averageMailboxSize.get();
    }

    public void setAverageMailboxSize(double value) {
        averageMailboxSize.set(value);
    }
}