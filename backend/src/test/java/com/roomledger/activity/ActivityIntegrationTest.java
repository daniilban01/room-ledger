package com.roomledger.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<String> activityTypes(String groupId, String token) throws Exception {
        JsonNode page = readJson(getWithToken("/api/groups/" + groupId + "/activity", token).andExpect(status().isOk()));
        List<String> types = new ArrayList<>();
        page.get("content").forEach(event -> types.add(event.get("type").asText()));
        return types;
    }

    @Test
    void recordsTheGroupHistoryNewestFirst() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        JsonNode group = createGroup(ana);
        String groupId = group.get("id").asText();
        joinWithCode(bogdan, group.get("joinCode").asText());
        String anaId = userId(ana);

        JsonNode expense = addEqualExpense(groupId, ana, "20.00", anaId, List.of(anaId));
        String expenseUrl = "/api/groups/" + groupId + "/expenses/" + expense.get("id").asText();

        Map<String, Object> edit = new HashMap<>();
        edit.put("description", "Cumpărături editate");
        edit.put("totalAmount", new BigDecimal("25.00"));
        edit.put("paidBy", anaId);
        edit.put("expenseDate", "2026-11-01");
        edit.put("splitType", "EQUAL");
        edit.put("participants", List.of(anaId));
        edit.put("version", 0);
        putJson(expenseUrl, edit, ana).andExpect(status().isOk());
        mockMvc.perform(delete(expenseUrl).header("Authorization", bearer(ana))).andExpect(status().isNoContent());

        assertThat(activityTypes(groupId, bogdan)).containsExactly(
                "EXPENSE_DELETED", "EXPENSE_EDITED", "EXPENSE_ADDED", "MEMBER_JOINED", "GROUP_CREATED");

        JsonNode latest = readJson(getWithToken("/api/groups/" + groupId + "/activity", ana)).get("content").get(0);
        assertThat(latest.get("actor").get("displayName").asText()).isEqualTo("Ana");
        assertThat(latest.get("payload").get("description").asText()).isEqualTo("Cumpărături editate");
        assertThat(latest.get("payload").get("amount").asText()).isEqualTo("25.00");
    }

    @Test
    void databaseRejectsChangesToTheLog() throws Exception {
        String ana = registerUser("Ana");
        createGroup(ana); // writes a GROUP_CREATED event

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE activity_events SET type = 'HACKED'"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM activity_events"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void nonMemberGets404() throws Exception {
        String ana = registerUser("Ana");
        String groupId = createGroup(ana).get("id").asText();
        String outsider = registerUser("Outsider");

        getWithToken("/api/groups/" + groupId + "/activity", outsider).andExpect(status().isNotFound());
    }
}
