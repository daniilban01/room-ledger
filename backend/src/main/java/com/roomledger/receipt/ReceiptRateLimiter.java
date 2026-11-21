package com.roomledger.receipt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At most N receipt scans per user in any rolling hour, to protect the API key's budget.
 *
 * <p>Sliding window: for each user we keep the times of their recent scans and forget the ones
 * older than an hour. Unlike a counter reset "every full hour", this cannot be gamed by sending
 * N requests at 10:59 and N more at 11:00.
 *
 * <p>In memory on purpose (the spec allows it): simple and fast, but the limits reset on restart
 * and are per instance. With several servers this would move to Redis or the database.
 */
@Component
public class ReceiptRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final int maxPerWindow;
    private final Clock clock;
    private final Map<UUID, Deque<Instant>> scansByUser = new ConcurrentHashMap<>();

    public ReceiptRateLimiter(@Value("${app.receipts.max-per-hour}") int maxPerWindow, Clock clock) {
        this.maxPerWindow = maxPerWindow;
        this.clock = clock;
    }

    /** Records a scan if allowed. Returns empty when allowed, or how long to wait when the limit is reached. */
    public Optional<Duration> tryAcquire(UUID userId) {
        Deque<Instant> scans = scansByUser.computeIfAbsent(userId, id -> new ArrayDeque<>());
        // Lock per user: two requests from the same user must not both see "9 of 10 used".
        synchronized (scans) {
            Instant now = clock.instant();
            while (!scans.isEmpty() && !scans.peekFirst().isAfter(now.minus(WINDOW))) {
                scans.pollFirst();
            }
            if (scans.size() >= maxPerWindow) {
                return Optional.of(Duration.between(now, scans.peekFirst().plus(WINDOW)));
            }
            scans.addLast(now);
            return Optional.empty();
        }
    }
}
