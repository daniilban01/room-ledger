package com.roomledger.receipt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Bound from app.llm.*: where the vision model lives, which model, and the key. Swapping provider = changing these. */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(String baseUrl, String model, String apiKey, Duration timeout) {
}
