package com.roomledger.receipt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@Profile("!dev-fake")
public class LlmClientConfig {

    @Bean
    RestClient llmRestClient(RestClient.Builder builder, LlmProperties properties) {
        // Fail at startup with a clear message instead of on the first receipt.
        if (isBlank(properties.apiKey()) || isBlank(properties.model()) || isBlank(properties.baseUrl())) {
            throw new IllegalStateException(
                    "Receipt scanning needs LLM_BASE_URL, LLM_MODEL and LLM_API_KEY. "
                            + "To run without them, start with SPRING_PROFILES_ACTIVE=dev-fake.");
        }

        // Explicit timeouts: without them a hanging provider would block a server thread indefinitely.
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(properties.timeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
