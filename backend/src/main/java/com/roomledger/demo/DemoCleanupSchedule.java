package com.roomledger.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/** Every night, removes the demo sandboxes older than a day so they do not pile up. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoCleanupSchedule {

    static final Duration MAX_AGE = Duration.ofDays(1);

    private static final Logger log = LoggerFactory.getLogger(DemoCleanupSchedule.class);

    private final DemoDataSeeder seeder;

    public DemoCleanupSchedule(DemoDataSeeder seeder) {
        this.seeder = seeder;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Bucharest")
    public void deleteOldSandboxes() {
        log.info("Deleted {} demo groups older than {}", seeder.deleteSandboxesOlderThan(MAX_AGE), MAX_AGE);
    }
}
