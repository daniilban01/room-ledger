package com.roomledger.auth;

import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerReturnsTokenThatAuthenticatesMe() throws Exception {
        String email = uniqueEmail();

        String token = readJson(postJson("/api/auth/register",
                Map.of("email", email, "password", PASSWORD, "displayName", "Ana"))
                .andExpect(status().isCreated()))
                .get("accessToken").asText();

        mockMvc.perform(get("/api/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Ana"));
    }

    @Test
    void registeringTheSameEmailTwiceReturns409IgnoringCase() throws Exception {
        String email = uniqueEmail();
        postJson("/api/auth/register", Map.of("email", email, "password", PASSWORD, "displayName", "Ana"))
                .andExpect(status().isCreated());

        postJson("/api/auth/register", Map.of("email", email.toUpperCase(), "password", PASSWORD, "displayName", "Ana"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        postJson("/api/auth/register", Map.of("email", "not-an-email", "password", "short", "displayName", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        String email = uniqueEmail();
        postJson("/api/auth/register", Map.of("email", email, "password", PASSWORD, "displayName", "Ana"))
                .andExpect(status().isCreated());

        String token = readJson(postJson("/api/auth/login", Map.of("email", email, "password", PASSWORD))
                .andExpect(status().isOk()))
                .get("accessToken").asText();

        assertThat(token).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        postJson("/api/auth/register", Map.of("email", email, "password", PASSWORD, "displayName", "Ana"))
                .andExpect(status().isCreated());

        postJson("/api/auth/login", Map.of("email", email, "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturnsSame401() throws Exception {
        postJson("/api/auth/login", Map.of("email", uniqueEmail(), "password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", bearer("not.a.valid.token")))
                .andExpect(status().isUnauthorized());
    }
}
