package com.rukamini.eat.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window attempt counter keyed by caller and endpoint. State is in memory, so each
 * instance of the application throttles independently.
 */
public class AuthRateLimiter {

    private static final int PURGE_THRESHOLD = 10_000;

    private record Window(long startMillis, int count) {
    }

    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimiter(int maxAttempts, long windowSeconds, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return maxAttempts > 0 && windowMillis > 0;
    }

    /** Records an attempt and returns 0 when allowed, or the seconds to wait before retrying. */
    public long acquire(String key) {
        if (!isEnabled()) {
            return 0;
        }
        long now = clock.millis();
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(w -> now - w.startMillis() >= windowMillis);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.startMillis() >= windowMillis
                        ? new Window(now, 1)
                        : new Window(current.startMillis(), current.count() + 1));
        if (window.count() <= maxAttempts) {
            return 0;
        }
        return Math.max(1, (window.startMillis() + windowMillis - now + 999) / 1000);
    }

    int trackedKeys() {
        return windows.size();
    }
}
