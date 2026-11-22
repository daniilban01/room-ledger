package com.roomledger.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.demo.enabled=true")
class DemoIntegrationTest extends IntegrationTest {

    @Autowired
    private DemoDataSeeder seeder;

    private String demoLogin() throws Exception {
        return readJson(mockMvc.perform(post("/api/auth/demo")).andExpect(status().isOk()))
                .get("accessToken").asText();
    }

    private JsonNode myGroups(String token) throws Exception {
        return readJson(getWithToken("/api/groups", token).andExpect(status().isOk()));
    }

    @Test
    void demoLoginOpensASeededGroupWhereAlexIsAdmin() throws Exception {
        String token = demoLogin();

        JsonNode groups = myGroups(token);
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).get("name").asText()).isEqualTo(DemoDataSeeder.GROUP_NAME);
        assertThat(groups.get(0).get("myRole").asText()).isEqualTo("ADMIN");

        String groupId = groups.get(0).get("id").asText();
        JsonNode balances = readJson(getWithToken("/api/groups/" + groupId + "/balances", token));
        // 3380.00 paid - 1035.20 owed - 400.00 received from Radu (confirmed)
        assertThat(balances.get("myBalance").decimalValue()).isEqualByComparingTo("1944.80");
        BigDecimal sum = StreamSupport.stream(balances.get("members").spliterator(), false)
                .map(member -> member.get("balance").decimalValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("0");

        JsonNode settlements = readJson(getWithToken("/api/groups/" + groupId + "/settlements", token));
        assertThat(settlements.findValuesAsText("status"))
                .containsExactlyInAnyOrder("CONFIRMED", "PENDING", "REJECTED");
    }

    @Test
    void everyDemoLoginGetsItsOwnSandbox() throws Exception {
        String first = demoLogin();
        String second = demoLogin();

        assertThat(userId(first)).isNotEqualTo(userId(second));
        assertThat(myGroups(first)).hasSize(1);
        assertThat(myGroups(first).get(0).get("id").asText())
                .isNotEqualTo(myGroups(second).get(0).get("id").asText());
    }

    @Test
    void cleanupDeletesDemoSandboxesButNotRealGroups() throws Exception {
        String demo = demoLogin();
        String realUser = registerUser("Ana");
        createGroup(realUser);

        seeder.deleteSandboxesOlderThan(Duration.ZERO);

        assertThat(myGroups(demo)).isEmpty();
        assertThat(myGroups(realUser)).hasSize(1);
    }
}
