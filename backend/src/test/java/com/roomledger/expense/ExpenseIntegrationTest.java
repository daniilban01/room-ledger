package com.roomledger.expense;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseIntegrationTest extends IntegrationTest {

    /** A group where Ana is ADMIN and Bogdan is MEMBER. */
    private record TestGroup(String id, String ana, String anaId, String bogdan, String bogdanId) {

        String expensesUrl() {
            return "/api/groups/" + id + "/expenses";
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

    private static Map<String, Object> expense(String total, String paidBy, String splitType) {
        Map<String, Object> body = new HashMap<>();
        body.put("description", "Cumpărături");
        body.put("totalAmount", new BigDecimal(total));
        body.put("paidBy", paidBy);
        body.put("expenseDate", "2026-11-01");
        body.put("splitType", splitType);
        return body;
    }

    private static Map<String, Object> equalExpense(String total, String paidBy, List<String> participants) {
        Map<String, Object> body = expense(total, paidBy, "EQUAL");
        body.put("participants", participants);
        return body;
    }

    private JsonNode create(TestGroup group, String token, Map<String, Object> body) throws Exception {
        return readJson(postJson(group.expensesUrl(), body, token).andExpect(status().isCreated()));
    }

    private static Map<String, BigDecimal> sharesByUser(JsonNode expense) {
        Map<String, BigDecimal> shares = new HashMap<>();
        expense.get("shares").forEach(share ->
                shares.put(share.get("userId").asText(), share.get("amount").decimalValue()));
        return shares;
    }

    @Test
    void equalSplitGivesLeftoverCentAndSumsToTotal() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();

        JsonNode created = create(group, group.ana(),
                equalExpense("10.01", group.anaId(), List.of(group.anaId(), group.bogdanId())));

        Map<String, BigDecimal> shares = sharesByUser(created);
        assertThat(shares.values()).usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("5.01"), new BigDecimal("5.00"));
        assertThat(created.get("version").asLong()).isZero();
        assertThat(created.get("createdBy").get("id").asText()).isEqualTo(group.anaId());
    }

    @Test
    void itemsSplitSumsEachPersonsItems() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = expense("52.50", group.anaId(), "ITEMS");
        body.put("items", List.of(
                Map.of("name", "Pizza", "amount", new BigDecimal("40.00"), "assignedTo", List.of(group.anaId(), group.bogdanId())),
                Map.of("name", "Bere", "amount", new BigDecimal("12.50"), "assignedTo", List.of(group.bogdanId()))));

        JsonNode created = create(group, group.ana(), body);

        Map<String, BigDecimal> shares = sharesByUser(created);
        assertThat(shares.get(group.anaId())).isEqualByComparingTo("20.00");
        assertThat(shares.get(group.bogdanId())).isEqualByComparingTo("32.50");
        assertThat(created.get("items")).hasSize(2);
        assertThat(created.get("items").get(0).get("name").asText()).isEqualTo("Pizza");
    }

    @Test
    void exactSharesThatDoNotAddUpReturn400() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = expense("100.00", group.anaId(), "EXACT");
        body.put("shares", List.of(
                Map.of("userId", group.anaId(), "amount", new BigDecimal("60.00")),
                Map.of("userId", group.bogdanId(), "amount", new BigDecimal("30.00"))));

        postJson(group.expensesUrl(), body, group.ana())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Shares must add up to the total 100.00, but they add up to 90.00"));
    }

    @Test
    void fieldsThatDoNotMatchSplitTypeReturn400() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = expense("10.00", group.anaId(), "EQUAL");
        body.put("shares", List.of(Map.of("userId", group.anaId(), "amount", new BigDecimal("10.00"))));

        postJson(group.expensesUrl(), body, group.ana()).andExpect(status().isBadRequest());
    }

    @Test
    void amountWithMoreThanTwoDecimalsReturns400() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();

        postJson(group.expensesUrl(), equalExpense("10.001", group.anaId(), List.of(group.anaId())), group.ana())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.totalAmount").exists());
    }

    @Test
    void payerOrParticipantOutsideTheGroupReturns400() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String outsiderId = userId(registerUser("Outsider"));

        postJson(group.expensesUrl(), equalExpense("10.00", outsiderId, List.of(group.anaId())), group.ana())
                .andExpect(status().isBadRequest());
        postJson(group.expensesUrl(), equalExpense("10.00", group.anaId(), List.of(group.anaId(), outsiderId)), group.ana())
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMemberGets404ForListAndDetail() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String expenseId = create(group, group.ana(),
                equalExpense("10.00", group.anaId(), List.of(group.anaId()))).get("id").asText();
        String outsider = registerUser("Outsider");

        getWithToken(group.expensesUrl(), outsider).andExpect(status().isNotFound());
        getWithToken(group.expensesUrl() + "/" + expenseId, outsider).andExpect(status().isNotFound());
    }

    @Test
    void memberCannotChangeSomeoneElsesExpense() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = equalExpense("10.00", group.anaId(), List.of(group.anaId(), group.bogdanId()));
        String expenseId = create(group, group.ana(), body).get("id").asText();
        body.put("version", 0);

        putJson(group.expensesUrl() + "/" + expenseId, body, group.bogdan()).andExpect(status().isForbidden());
        mockMvc.perform(delete(group.expensesUrl() + "/" + expenseId).header("Authorization", bearer(group.bogdan())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditAnyExpense() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = equalExpense("10.00", group.bogdanId(), List.of(group.anaId(), group.bogdanId()));
        String expenseId = create(group, group.bogdan(), body).get("id").asText();

        body.put("totalAmount", new BigDecimal("20.00"));
        body.put("version", 0);
        JsonNode updated = readJson(putJson(group.expensesUrl() + "/" + expenseId, body, group.ana())
                .andExpect(status().isOk()));

        assertThat(updated.get("totalAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(updated.get("version").asLong()).isEqualTo(1);
        assertThat(sharesByUser(updated).get(group.anaId())).isEqualByComparingTo("10.00");
    }

    @Test
    void updateCanChangeSplitTypeAndParticipants() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = equalExpense("30.00", group.anaId(), List.of(group.anaId(), group.bogdanId()));
        String expenseId = create(group, group.ana(), body).get("id").asText();

        Map<String, Object> exact = expense("30.00", group.anaId(), "EXACT");
        exact.put("shares", List.of(Map.of("userId", group.bogdanId(), "amount", new BigDecimal("30.00"))));
        exact.put("version", 0);
        JsonNode updated = readJson(putJson(group.expensesUrl() + "/" + expenseId, exact, group.ana())
                .andExpect(status().isOk()));

        assertThat(sharesByUser(updated)).containsOnlyKeys(group.bogdanId());
    }

    /**
     * Two people open the same expense (version 0). The first save wins and bumps the version to 1;
     * the second save still says "I edited version 0", so it is rejected instead of silently
     * overwriting the first change (the "lost update" problem).
     */
    @Test
    void concurrentUpdateWithStaleVersionReturns409() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = equalExpense("10.00", group.anaId(), List.of(group.anaId(), group.bogdanId()));
        String expenseUrl = group.expensesUrl() + "/" + create(group, group.ana(), body).get("id").asText();
        body.put("version", 0);

        body.put("description", "First edit");
        putJson(expenseUrl, body, group.ana()).andExpect(status().isOk());

        body.put("description", "Second edit, based on the old version");
        putJson(expenseUrl, body, group.ana()).andExpect(status().isConflict());

        getWithToken(expenseUrl, group.ana())
                .andExpect(jsonPath("$.description").value("First edit"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateWithoutVersionReturns400() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        Map<String, Object> body = equalExpense("10.00", group.anaId(), List.of(group.anaId()));
        String expenseId = create(group, group.ana(), body).get("id").asText();

        putJson(group.expensesUrl() + "/" + expenseId, body, group.ana()).andExpect(status().isBadRequest());
    }

    @Test
    void deletedExpenseDisappearsFromListAndDetail() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        String expenseId = create(group, group.ana(),
                equalExpense("10.00", group.anaId(), List.of(group.anaId()))).get("id").asText();

        mockMvc.perform(delete(group.expensesUrl() + "/" + expenseId).header("Authorization", bearer(group.ana())))
                .andExpect(status().isNoContent());

        getWithToken(group.expensesUrl() + "/" + expenseId, group.ana()).andExpect(status().isNotFound());
        getWithToken(group.expensesUrl(), group.ana()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listIsPaginatedNewestFirst() throws Exception {
        TestGroup group = groupWithAnaAndBogdan();
        for (String date : List.of("2026-10-01", "2026-10-03", "2026-10-02")) {
            Map<String, Object> body = equalExpense("10.00", group.anaId(), List.of(group.anaId()));
            body.put("expenseDate", date);
            body.put("description", "Expense " + date);
            create(group, group.ana(), body);
        }

        getWithToken(group.expensesUrl() + "?page=0&size=2", group.ana())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].description").value("Expense 2026-10-03"))
                .andExpect(jsonPath("$.content[1].description").value("Expense 2026-10-02"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
