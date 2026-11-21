package com.roomledger.receipt;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests the real parser against a fake HTTP server: we check exactly what is sent to the provider
 * and how its answer is read, without network access, API keys or costs.
 */
class OpenAiCompatibleReceiptParserTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private MockRestServiceServer server;
    private OpenAiCompatibleReceiptParser parser;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://llm.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        // Zero backoff: the retry logic runs for real, but the test does not wait between attempts.
        parser = new OpenAiCompatibleReceiptParser(
                builder.build(),
                new LlmProperties("https://llm.test/v1", "vision-model", "secret-key", Duration.ofSeconds(5)),
                Duration.ZERO);
    }

    private static String answer(String receiptJson) {
        return """
                {"choices": [{"message": {"role": "assistant", "content": %s}}]}
                """.formatted(TextNode.valueOf(receiptJson).toString()); // JSON-escapes the receipt into a string
    }

    @Test
    void sendsTheImageAndReadsTheAnswer() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-key"))
                .andExpect(jsonPath("$.model").value("vision-model"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content[1].image_url.url").value(startsWith("data:image/png;base64,")))
                .andRespond(withSuccess("""
                        {"choices": [{"message": {"role": "assistant",
                          "content": "{\\"storeName\\": \\"Lidl\\", \\"items\\": [{\\"name\\": \\"Apă\\", \\"amount\\": 2.99}], \\"total\\": 2.99}"}}]}
                        """, MediaType.APPLICATION_JSON));

        ParsedReceipt receipt = parser.parse(PNG, "image/png");

        assertThat(receipt.storeName()).isEqualTo("Lidl");
        assertThat(receipt.items()).hasSize(1);
        assertThat(receipt.total()).isEqualByComparingTo("2.99");
        server.verify();
    }

    @Test
    void retriesWhenTheProviderIsTemporarilyOverloaded() {
        // Expectations are ordered: first a 503, then a successful answer.
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(answer("{\"items\": [{\"name\": \"Apă\", \"amount\": 2.99}]}"),
                        MediaType.APPLICATION_JSON));

        ParsedReceipt receipt = parser.parse(PNG, "image/png");

        assertThat(receipt.items()).hasSize(1);
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttempts() {
        server.expect(ExpectedCount.times(OpenAiCompatibleReceiptParser.MAX_ATTEMPTS),
                        requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> parser.parse(PNG, "image/png")).isInstanceOf(ReceiptProviderException.class);
        server.verify();
    }

    @Test
    void doesNotRetryErrorsThatWouldFailAgain() {
        // 401 = wrong key: retrying cannot help. A second request would fail this test ("no further requests expected").
        server.expect(ExpectedCount.once(), requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> parser.parse(PNG, "image/png")).isInstanceOf(ReceiptProviderException.class);
        server.verify();
    }

    @Test
    void answerWithoutContentIsUnreadable() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> parser.parse(PNG, "image/png")).isInstanceOf(UnreadableReceiptException.class);
    }
}
