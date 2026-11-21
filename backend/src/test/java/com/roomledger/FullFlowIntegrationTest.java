package com.roomledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole product story through the real HTTP API, as a user would experience it:
 * register -> create group -> join -> add expense -> balances -> "I paid" -> confirm -> settled.
 */
class FullFlowIntegrationTest extends IntegrationTest {

    @Test
    void roommatesShareAnExpenseAndSettleUp() throws Exception {
        // 1. Two roommates register; Ana creates the group and Bogdan joins with the code.
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        String anaId = userId(ana);
        String bogdanId = userId(bogdan);
        JsonNode group = createGroup(ana);
        String groupId = group.get("id").asText();
        joinWithCode(bogdan, group.get("joinCode").asText());
        String groupUrl = "/api/groups/" + groupId;

        // 2. Ana pays 120.50 for groceries, split equally.
        addEqualExpense(groupId, ana, "120.50", anaId, List.of(anaId, bogdanId));

        // 3. Balances: Ana is owed 60.25, Bogdan owes 60.25, and they sum to zero.
        assertThat(myBalance(groupUrl, ana)).isEqualByComparingTo("60.25");
        assertThat(myBalance(groupUrl, bogdan)).isEqualByComparingTo("-60.25");

        // 4. The suggested settlement is a single transfer Bogdan -> Ana.
        JsonNode transfers = readJson(getWithToken(groupUrl + "/balances/simplified", bogdan)).get("transfers");
        assertThat(transfers).hasSize(1);
        assertThat(transfers.get(0).get("from").get("id").asText()).isEqualTo(bogdanId);
        assertThat(transfers.get(0).get("amount").decimalValue()).isEqualByComparingTo("60.25");

        // 5. Bogdan says "I paid": PENDING, balances do not move yet.
        String settlementId = readJson(postJson(groupUrl + "/settlements",
                Map.of("fromUserId", bogdanId, "toUserId", anaId, "amount", new BigDecimal("60.25")), bogdan)
                .andExpect(status().isCreated()))
                .get("id").asText();
        assertThat(myBalance(groupUrl, bogdan)).isEqualByComparingTo("-60.25");

        // 6. Ana confirms she received the money: everyone is settled.
        postJson(groupUrl + "/settlements/" + settlementId + "/confirm", Map.of(), ana).andExpect(status().isOk());
        assertThat(myBalance(groupUrl, ana)).isZero();
        assertThat(myBalance(groupUrl, bogdan)).isZero();
        assertThat(readJson(getWithToken(groupUrl + "/balances/simplified", ana)).get("transfers")).isEmpty();

        // 7. The activity log tells the whole story, newest first.
        JsonNode activity = readJson(getWithToken(groupUrl + "/activity", bogdan)).get("content");
        List<String> types = new ArrayList<>();
        activity.forEach(event -> types.add(event.get("type").asText()));
        assertThat(types).containsExactly(
                "SETTLEMENT_CONFIRMED", "SETTLEMENT_MARKED_PAID", "EXPENSE_ADDED", "MEMBER_JOINED", "GROUP_CREATED");
    }

    private BigDecimal myBalance(String groupUrl, String token) throws Exception {
        return readJson(getWithToken(groupUrl + "/balances", token).andExpect(status().isOk()))
                .get("myBalance").decimalValue();
    }
}
