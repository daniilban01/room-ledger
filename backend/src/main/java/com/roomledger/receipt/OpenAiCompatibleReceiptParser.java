package com.roomledger.receipt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Calls any provider that speaks the OpenAI "chat completions" format with image input
 * (OpenAI, Google Gemini, Anthropic, Ollama...). Only URL, model and key differ between them.
 */
@Component
@Profile("!dev-fake")
public class OpenAiCompatibleReceiptParser implements ReceiptParser {

    static final String SYSTEM_PROMPT = """
            You read photos of shop receipts, usually from Romania with prices in lei (RON).
            Reply with ONLY a JSON object, no markdown and no explanations, with exactly this shape:
            {"storeName": string or null, "date": "YYYY-MM-DD" or null,
             "items": [{"name": string, "amount": number}], "total": number or null}
            Rules:
            - One item per purchased product line, with the name as printed on the receipt.
            - "amount" is the final price paid for that line (quantity x unit price), as a number
              with a dot as decimal separator, e.g. 12.50.
            - If a discount line belongs to a product, subtract it from that product's amount.
            - Do not list subtotals, VAT, payment method, change or card lines as items.
            - "total" is the total to pay printed on the receipt.
            - Use null for anything you cannot read. Never invent products or prices.
            """;

    static final int MAX_ATTEMPTS = 3;
    // "Try again later" answers: overloaded (503), rate limited (429), or a transient server error.
    // Other errors (400 bad request, 401 wrong key, 404 unknown model) would fail the same way again.
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleReceiptParser.class);

    private final RestClient restClient;
    private final LlmProperties properties;
    private final Duration initialBackoff;
    private final ReceiptJsonReader jsonReader = new ReceiptJsonReader();

    @Autowired
    public OpenAiCompatibleReceiptParser(RestClient llmRestClient, LlmProperties properties) {
        this(llmRestClient, properties, Duration.ofSeconds(1));
    }

    /** Tests pass a zero backoff so they do not actually sleep. */
    OpenAiCompatibleReceiptParser(RestClient llmRestClient, LlmProperties properties, Duration initialBackoff) {
        this.restClient = llmRestClient;
        this.properties = properties;
        this.initialBackoff = initialBackoff;
    }

    @Override
    public ParsedReceipt parse(byte[] image, String mediaType) {
        // The image travels inline as a data URL, so nothing has to be uploaded or stored anywhere first.
        String dataUrl = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        ChatRequest request = new ChatRequest(
                properties.model(),
                0, // temperature 0: we want the most literal reading, not creativity
                new ResponseFormat("json_object"),
                List.of(
                        new Message("system", SYSTEM_PROMPT),
                        new Message("user", List.of(
                                new TextPart("text", "Read this receipt."),
                                new ImagePart("image_url", new ImageUrl(dataUrl))))));

        JsonNode response = callWithRetry(request);
        String content = response == null ? null : response.path("choices").path(0).path("message").path("content").asText(null);
        return jsonReader.read(content);
    }

    /**
     * Retry with exponential backoff: temporary failures (provider overloaded or rate limiting us)
     * often succeed a moment later, so we try up to MAX_ATTEMPTS times, waiting 1s, then 2s.
     * Waiting longer each time gives an overloaded provider room to recover instead of hammering it.
     * Timeouts are NOT retried: each attempt can already take up to the read timeout.
     */
    private JsonNode callWithRetry(ChatRequest request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return restClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (!RETRYABLE_STATUSES.contains(status) || attempt == MAX_ATTEMPTS) {
                    throw new ReceiptProviderException("The receipt reading service answered " + e.getStatusCode(), e);
                }
                log.info("Receipt provider answered {}, retrying (attempt {} of {})", status, attempt + 1, MAX_ATTEMPTS);
                sleep(initialBackoff.multipliedBy(1L << (attempt - 1)));
            } catch (RestClientException e) {
                // Connection refused, DNS failure, read timeout...
                throw new ReceiptProviderException("The receipt reading service could not be reached", e);
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // Restore the flag so whoever interrupted us (e.g. a shutting-down server) still sees it.
            Thread.currentThread().interrupt();
            throw new ReceiptProviderException("Interrupted while waiting to retry", e);
        }
    }

    // Request body of the chat completions API. Records + @JsonProperty keep the JSON shape explicit.
    record ChatRequest(String model, double temperature,
                       @JsonProperty("response_format") ResponseFormat responseFormat,
                       List<Message> messages) {
    }

    record ResponseFormat(String type) {
    }

    /** {@code content} is either a plain string or a list of parts (text + image). */
    record Message(String role, Object content) {
    }

    record TextPart(String type, String text) {
    }

    record ImagePart(String type, @JsonProperty("image_url") ImageUrl imageUrl) {
    }

    record ImageUrl(String url) {
    }
}
