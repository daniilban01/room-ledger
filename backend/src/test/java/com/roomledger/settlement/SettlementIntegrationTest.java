package com.roomledger.settlement;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomledger.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementIntegrationTest extends IntegrationTest {

    /** Ana (ADMIN) paid 30.00 split with Bogdan, so Bogdan owes Ana 15.00. Cristi is a third member. */
    private record Setup(String groupId, String ana, String anaId, String bogdan, String bogdanId,
                         String cristi, String cristiId) {

        String url(String path) {
            return "/api/groups/" + groupId + path;
        }
    }

    private Setup bogdanOwesAna15() throws Exception {
        String ana = registerUser("Ana");
        String bogdan = registerUser("Bogdan");
        String cristi = registerUser("Cristi");
        JsonNode group = createGroup(ana);
        joinWithCode(bogdan, group.get("joinCode").asText());
        joinWithCode(cristi, group.get("joinCode").asText());
        String groupId = group.get("id").asText();
        String anaId = userId(ana);
        String bogdanId = userId(bogdan);
        addEqualExpense(groupId, ana, "30.00", anaId, List.of(anaId, bogdanId));
        return new Setup(groupId, ana, anaId, bogdan, bogdanId, cristi, userId(cristi));
    }

    private static Map<String, Object> payment(String from, String to, String amount) {
        return Map.of("fromUserId", from, "toUserId", to, "amount", new BigDecimal(amount));
    }

    private BigDecimal balanceOf(Setup setup, String token) throws Exception {
        return readJson(getWithToken(setup.url("/balances"), token)).get("myBalance").decimalValue();
    }

    private String markPaid(Setup s) throws Exception {
        return readJson(postJson(s.url("/settlements"), payment(s.bogdanId(), s.anaId(), "15.00"), s.bogdan())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")))
                .get("id").asText();
    }

    private int postAs(String token, String url) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", bearer(token))).andReturn().getResponse().getStatus();
    }

    @Test
    void markingAsPaidDoesNotChangeBalancesUntilConfirmed() throws Exception {
        Setup s = bogdanOwesAna15();

        String settlementId = markPaid(s);
        assertThat(balanceOf(s, s.bogdan())).isEqualByComparingTo("-15.00");

        postJson(s.url("/settlements/" + settlementId + "/confirm"), Map.of(), s.ana())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
        assertThat(balanceOf(s, s.bogdan())).isZero();
        assertThat(balanceOf(s, s.ana())).isZero();
    }

    @Test
    void rejectedPaymentLeavesBalancesUnchanged() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);

        postJson(s.url("/settlements/" + settlementId + "/reject"), Map.of(), s.ana())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(balanceOf(s, s.bogdan())).isEqualByComparingTo("-15.00");
    }

    @Test
    void receiverCanRecordAPaymentDirectly() throws Exception {
        Setup s = bogdanOwesAna15();

        postJson(s.url("/settlements"), payment(s.bogdanId(), s.anaId(), "10.00"), s.ana())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(balanceOf(s, s.bogdan())).isEqualByComparingTo("-5.00");
    }

    @Test
    void simplifiedDebtsReflectConfirmedPayments() throws Exception {
        Setup s = bogdanOwesAna15();
        getWithToken(s.url("/balances/simplified"), s.ana())
                .andExpect(jsonPath("$.directDebts").value(1))
                .andExpect(jsonPath("$.transfers.length()").value(1));

        postJson(s.url("/settlements"), payment(s.bogdanId(), s.anaId(), "15.00"), s.ana())
                .andExpect(status().isCreated());

        getWithToken(s.url("/balances/simplified"), s.ana())
                .andExpect(jsonPath("$.directDebts").value(0))
                .andExpect(jsonPath("$.transfers.length()").value(0));
    }

    @Test
    void detailsIncludeNoteAndTheExpensesThePayerOwesFrom() throws Exception {
        Setup s = bogdanOwesAna15();
        Map<String, Object> body = new java.util.HashMap<>(payment(s.bogdanId(), s.anaId(), "15.00"));
        body.put("note", "  Cumpărăturile de luni  ");
        String settlementId = readJson(postJson(s.url("/settlements"), body, s.bogdan())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Cumpărăturile de luni")))
                .get("id").asText();

        JsonNode details = readJson(getWithToken(s.url("/settlements/" + settlementId), s.ana())
                .andExpect(status().isOk()));

        assertThat(details.get("settlement").get("note").asText()).isEqualTo("Cumpărăturile de luni");
        JsonNode owed = details.get("payerOwesFrom");
        assertThat(owed).hasSize(1);
        assertThat(owed.get(0).get("paidBy").get("id").asText()).isEqualTo(s.anaId());
        assertThat(owed.get(0).get("share").decimalValue()).isEqualByComparingTo("15.00");
    }

    @Test
    void noteLongerThan200CharactersReturns400() throws Exception {
        Setup s = bogdanOwesAna15();
        Map<String, Object> body = new java.util.HashMap<>(payment(s.bogdanId(), s.anaId(), "15.00"));
        body.put("note", "x".repeat(201));

        postJson(s.url("/settlements"), body, s.bogdan())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.note").exists());
    }

    @Test
    void payerCannotConfirmTheirOwnPayment() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);

        assertThat(postAs(s.bogdan(), s.url("/settlements/" + settlementId + "/confirm"))).isEqualTo(403);
    }

    @Test
    void anotherMemberCannotConfirmOrReject() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);

        assertThat(postAs(s.cristi(), s.url("/settlements/" + settlementId + "/confirm"))).isEqualTo(403);
        assertThat(postAs(s.cristi(), s.url("/settlements/" + settlementId + "/reject"))).isEqualTo(403);
    }

    @Test
    void cannotRecordAPaymentBetweenTwoOtherPeople() throws Exception {
        Setup s = bogdanOwesAna15();

        postJson(s.url("/settlements"), payment(s.bogdanId(), s.anaId(), "15.00"), s.cristi())
                .andExpect(status().isForbidden());
    }

    @Test
    void resolvedSettlementCannotChangeAgain() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);
        postJson(s.url("/settlements/" + settlementId + "/confirm"), Map.of(), s.ana()).andExpect(status().isOk());

        assertThat(postAs(s.ana(), s.url("/settlements/" + settlementId + "/reject"))).isEqualTo(409);
        assertThat(postAs(s.ana(), s.url("/settlements/" + settlementId + "/confirm"))).isEqualTo(409);
    }

    @Test
    void invalidPaymentsReturn400() throws Exception {
        Setup s = bogdanOwesAna15();
        String outsiderId = userId(registerUser("Outsider"));

        postJson(s.url("/settlements"), payment(s.bogdanId(), s.bogdanId(), "5.00"), s.bogdan())
                .andExpect(status().isBadRequest());
        postJson(s.url("/settlements"), payment(s.bogdanId(), s.anaId(), "0.00"), s.bogdan())
                .andExpect(status().isBadRequest());
        postJson(s.url("/settlements"), payment(s.bogdanId(), outsiderId, "5.00"), s.bogdan())
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMemberGets404() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);
        String outsider = registerUser("Outsider");

        getWithToken(s.url("/settlements"), outsider).andExpect(status().isNotFound());
        getWithToken(s.url("/settlements/" + settlementId), outsider).andExpect(status().isNotFound());
        assertThat(postAs(outsider, s.url("/settlements/" + settlementId + "/confirm"))).isEqualTo(404);
    }

    /**
     * Confirm and reject the same PENDING settlement at the same moment from two threads.
     * Whichever commits first wins; the other one either sees the settlement already resolved
     * (state check) or loses the @Version race at flush time. Both cases must answer 409.
     */
    @Test
    void concurrentConfirmAndRejectLetOnlyOneWin() throws Exception {
        Setup s = bogdanOwesAna15();
        String settlementId = markPaid(s);
        String base = s.url("/settlements/" + settlementId);

        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> actions = List.of(
                () -> { start.await(); return postAs(s.ana(), base + "/confirm"); },
                () -> { start.await(); return postAs(s.ana(), base + "/reject"); });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            actions.forEach(action -> futures.add(executor.submit(action)));
            start.countDown(); // release both threads at once

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }

        String finalStatus = readJson(getWithToken(s.url("/settlements"), s.ana())).get(0).get("status").asText();
        assertThat(finalStatus).isIn("CONFIRMED", "REJECTED");
    }
}
