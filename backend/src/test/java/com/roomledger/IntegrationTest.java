package com.roomledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: full Spring context + real PostgreSQL (Testcontainers).
 * All subclasses share the same configuration, so Spring caches one context and one container
 * for the whole test run. Tests therefore use unique emails instead of cleaning the database.
 */
@SpringBootTest(properties = "app.jwt.secret=test-secret-that-is-long-enough-for-hs256")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.ro";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions postJson(String url, Object body, String token) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions putJson(String url, Object body, String token) throws Exception {
        return mockMvc.perform(put(url)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions getWithToken(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(token)));
    }

    /** The id of the user that owns this token, read from GET /api/me. */
    protected String userId(String token) throws Exception {
        return readJson(getWithToken("/api/me", token).andExpect(status().isOk())).get("id").asText();
    }

    protected JsonNode readJson(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Registers a new user and returns their access token. */
    protected String registerUser(String displayName) throws Exception {
        var body = Map.of("email", uniqueEmail(), "password", PASSWORD, "displayName", displayName);
        ResultActions result = postJson("/api/auth/register", body).andExpect(status().isCreated());
        return readJson(result).get("accessToken").asText();
    }
}
