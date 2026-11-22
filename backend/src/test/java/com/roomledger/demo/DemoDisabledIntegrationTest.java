package com.roomledger.demo;

import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs with the default configuration, where the demo is off. */
class DemoDisabledIntegrationTest extends IntegrationTest {

    @Test
    void demoLoginReturns404WhenTheDemoIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/demo")).andExpect(status().isNotFound());
    }
}
