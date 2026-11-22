package com.roomledger.demo;

import com.roomledger.auth.JwtService;
import com.roomledger.auth.dto.TokenResponse;
import com.roomledger.common.error.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * One-click login for people trying the app without registering: every call creates a private
 * sandbox (see {@link DemoDataSeeder}). When the demo is disabled this bean does not exist, so the
 * endpoint answers 404.
 */
@RestController
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoController {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final DemoDataSeeder seeder;
    private final JwtService jwtService;
    private final Clock clock;
    private final int maxPerHour;
    // Global, not per visitor: the endpoint is public, so this caps how fast anyone can fill the
    // database with sandboxes. Sliding window, same idea as the receipt rate limiter.
    private final Deque<Instant> recent = new ArrayDeque<>();

    public DemoController(DemoDataSeeder seeder, JwtService jwtService, Clock clock,
                          @Value("${app.demo.max-per-hour}") int maxPerHour) {
        this.seeder = seeder;
        this.jwtService = jwtService;
        this.clock = clock;
        this.maxPerHour = maxPerHour;
    }

    @PostMapping("/api/auth/demo")
    public TokenResponse loginAsDemo() {
        acquire();
        return new TokenResponse(jwtService.generateToken(seeder.createSandbox()));
    }

    private void acquire() {
        synchronized (recent) {
            Instant now = clock.instant();
            while (!recent.isEmpty() && !recent.peekFirst().isAfter(now.minus(WINDOW))) {
                recent.pollFirst();
            }
            if (recent.size() >= maxPerHour) {
                throw new TooManyRequestsException("Too many demo accounts created in the last hour",
                        Duration.between(now, recent.peekFirst().plus(WINDOW)));
            }
            recent.addLast(now);
        }
    }
}
