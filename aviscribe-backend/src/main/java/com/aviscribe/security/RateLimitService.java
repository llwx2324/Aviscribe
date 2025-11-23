package com.aviscribe.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class RateLimitService {

    private static class Counter {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long windowStart;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return true;
        }
        long now = Instant.now().getEpochSecond();
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = now;
            return c;
        });

        synchronized (counter) {
            if (now - counter.windowStart >= windowSeconds) {
                counter.windowStart = now;
                counter.count.set(0);
            }
            if (counter.count.incrementAndGet() > maxRequests) {
                return false;
            }
            return true;
        }
    }

    public void reset() {
        counters.clear();
    }
}
