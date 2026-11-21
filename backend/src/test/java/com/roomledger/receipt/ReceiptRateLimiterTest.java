package com.roomledger.receipt;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptRateLimiterTest {

    /** A clock the test can move forward, instead of waiting an hour for real. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-21T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final TestClock clock = new TestClock();
    private final ReceiptRateLimiter limiter = new ReceiptRateLimiter(3, clock);
    private final UUID user = UUID.randomUUID();

    @Test
    void allowsUpToTheLimitThenTellsHowLongToWait() {
        assertThat(limiter.tryAcquire(user)).isEmpty();
        clock.advance(Duration.ofMinutes(10));
        assertThat(limiter.tryAcquire(user)).isEmpty();
        assertThat(limiter.tryAcquire(user)).isEmpty();

        // The oldest scan was 10 minutes ago, so a slot frees up in 50 minutes.
        assertThat(limiter.tryAcquire(user)).contains(Duration.ofMinutes(50));
    }

    @Test
    void slotsFreeUpAsTheWindowSlides() {
        limiter.tryAcquire(user);
        limiter.tryAcquire(user);
        limiter.tryAcquire(user);

        clock.advance(Duration.ofMinutes(60));

        assertThat(limiter.tryAcquire(user)).isEmpty();
    }

    @Test
    void limitsEachUserSeparately() {
        limiter.tryAcquire(user);
        limiter.tryAcquire(user);
        limiter.tryAcquire(user);

        assertThat(limiter.tryAcquire(UUID.randomUUID())).isEmpty();
    }
}
