package com.rukamini.eat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsUpToTheLimitThenReportsRetryAfter() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(3, 60, clock);
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.acquire("a"));
        }
        assertEquals(60, limiter.acquire("a"));
        clock.advance(Duration.ofSeconds(10));
        assertEquals(50, limiter.acquire("a"));
    }

    @Test
    void keysAreIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter(1, 60, new MutableClock());
        assertEquals(0, limiter.acquire("a"));
        assertTrue(limiter.acquire("a") > 0);
        assertEquals(0, limiter.acquire("b"));
    }

    @Test
    void windowResetsAfterItExpires() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(1, 60, clock);
        assertEquals(0, limiter.acquire("a"));
        assertTrue(limiter.acquire("a") > 0);
        clock.advance(Duration.ofSeconds(60));
        assertEquals(0, limiter.acquire("a"));
    }

    @Test
    void nonPositiveSettingsDisableLimiting() {
        assertFalse(new AuthRateLimiter(0, 60, new MutableClock()).isEnabled());
        assertFalse(new AuthRateLimiter(5, 0, new MutableClock()).isEnabled());
        AuthRateLimiter disabled = new AuthRateLimiter(0, 60, new MutableClock());
        for (int i = 0; i < 100; i++) {
            assertEquals(0, disabled.acquire("a"));
        }
        assertEquals(0, disabled.trackedKeys());
    }

    @Test
    void expiredEntriesArePurgedWhenTheMapGrowsLarge() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(5, 60, clock);
        for (int i = 0; i < 10_001; i++) {
            limiter.acquire("k" + i);
        }
        assertEquals(10_001, limiter.trackedKeys());
        clock.advance(Duration.ofSeconds(61));
        limiter.acquire("fresh");
        assertEquals(1, limiter.trackedKeys());
    }
}
