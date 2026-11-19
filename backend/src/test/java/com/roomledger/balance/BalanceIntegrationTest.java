package com.roomledger.balance;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BalanceIntegrationTest extends IntegrationTest {

    private record TestGroup(String id, String ana, String anaId, String bogdan, String bogdanId) {

        String url(String path) {
            return "/api/groups/" + id + path;
        }
    }

    private TestGroup groupWithAnaAndBogdan() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        JsonNode group = readJson(postJson("/api/groups", Map.of("name", "Apartament"), ana)
                .andExpect(status().isCreated()));
        postJson("/api/groups/join", Map.of("joinCode", group.get("joinCode").asText()), bogdan)
                .andExpect(status().isOk());
        return new TestGroup(group.get("id").asText(), ana, userId(ana), bogdan, userId(bogdan));
    }

    private String addEqualExpense(TestGroup group, String token, String total, String paidBy, List<String> participants)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("description", "Cheltuială");
        body.put("totalAmount", new BigDecimal(total));
        body.put("paidBy", paidBy);
        body.put("expenseDate", "2026-11-01");
        body.put("splitType", "EQUAL");
        body.put("participants", participants);
        return readJson(postJson(group.url("/expenses"), body, token).andExpect(status().isCreated()))
                .get("id").asText();
    }

    private Map<String, BigDecimal> balancesByUser(JsonNode response) {
        Map<String, BigDecimal> balances = new HashMap<>();
        response.get("members").forEach(member ->
                balances.put(member.get("userId").asText(), member.get("balance").decimalValue()));
        return balances;
    }

    @Test
    void balancesReflectWhoPaidAndWhoOwes() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        List<String> both = List.of(group.anaId(), group.bogdanId());
        addEqualExpense(group, group.ana(), "30.00", group.anaId(), both);       // Ana +15, Bogdan -15
        addEqualExpense(group, group.bogdan(), "10.00", group.bogdanId(), both); // Ana -5,  Bogdan +5

        JsonNode response = readJson(getWithToken(group.url("/balances"), group.ana()).andExpect(status().isOk()));

        Map<String, BigDecimal> balances = balancesByUser(response);
        assertThat(balances.get(group.anaId())).isEqualByComparingTo("10.00");
        assertThat(balances.get(group.bogdanId())).isEqualByComparingTo("-10.00");
        assertThat(response.get("myBalance").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isZero();
    }

    @Test
    void memberWithoutExpensesHasZeroBalance() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();

        JsonNode response = readJson(getWithToken(group.url("/balances"), group.bogdan()).andExpect(status().isOk()));

        assertThat(balancesByUser(response).values()).allSatisfy(balance -> assertThat(balance).isZero());
        assertThat(response.get("myBalance").decimalValue()).isZero();
    }

    @Test
    void balancesStayZeroSumWithLeftoverCents() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String cristi = registerUser("Cristi");
        String code = readJson(getWithToken("/api/groups/" + group.id(), group.ana())).get("joinCode").asText();
        postJson("/api/groups/join", Map.of("joinCode", code), cristi).andExpect(status().isOk());
        addEqualExpense(group, group.ana(), "100.00", group.anaId(), List.of(group.anaId(), group.bogdanId(), userId(cristi)));

        JsonNode response = readJson(getWithToken(group.url("/balances"), group.ana()));

        assertThat(balancesByUser(response).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isZero();
    }

    @Test
    void deletedExpensesDoNotCount() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String expenseId = addEqualExpense(group, group.ana(), "30.00", group.anaId(), List.of(group.anaId(), group.bogdanId()));

        mockMvc.perform(delete(group.url("/expenses/" + expenseId)).header("Authorization", bearer(group.ana())))
                .andExpect(status().isNoContent());

        JsonNode response = readJson(getWithToken(group.url("/balances"), group.ana()));
        assertThat(balancesByUser(response).values()).allSatisfy(balance -> assertThat(balance).isZero());
    }

    @Test
    void simplifiedDebtsSuggestWhoPaysWhom() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        addEqualExpense(group, group.ana(), "30.00", group.anaId(), List.of(group.anaId(), group.bogdanId()));

        JsonNode response = readJson(getWithToken(group.url("/balances/simplified"), group.bogdan())
                .andExpect(status().isOk()));

        JsonNode transfers = response.get("transfers");
        assertThat(transfers).hasSize(1);
        assertThat(transfers.get(0).get("from").get("id").asText()).isEqualTo(group.bogdanId());
        assertThat(transfers.get(0).get("to").get("id").asText()).isEqualTo(group.anaId());
        assertThat(transfers.get(0).get("amount").decimalValue()).isEqualByComparingTo("15.00");
    }

    @Test
    void nonMemberGets404() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String outsider = registerUser("Outsider");

        getWithToken(group.url("/balances"), outsider).andExpect(status().isNotFound());
        getWithToken(group.url("/balances/simplified"), outsider).andExpect(status().isNotFound());
    }
}
