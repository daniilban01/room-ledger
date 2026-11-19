package com.roomledger.balance;

import com.roomledger.balance.DebtSimplifier.Transfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000000");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000000");
    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000000");
    private static final UUID D = UUID.fromString("40000000-0000-0000-0000-000000000000");

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static Map<UUID, BigDecimal> balances(Object... userAndAmount) {
        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        for (int i = 0; i < userAndAmount.length; i += 2) {
            balances.put((UUID) userAndAmount[i], money((String) userAndAmount[i + 1]));
        }
        return balances;
    }

    private static Transfer transfer(UUID from, UUID to, String amount) {
        return new Transfer(from, to, money(amount));
    }

    @Test
    void noTransfersWhenEveryoneIsSettled() {
        assertThat(DebtSimplifier.simplify(balances(A, "0.00", B, "0.00"))).isEmpty();
        assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
    }

    @Test
    void oneDebtorPaysOneCreditor() {
        assertThat(DebtSimplifier.simplify(balances(A, "25.50", B, "-25.50")))
                .containsExactly(transfer(B, A, "25.50"));
    }

    @Test
    void chainCollapsesIntoOneTransfer() {
        // A owes B 10 and B owes C 10: B is even, so A pays C directly.
        assertThat(DebtSimplifier.simplify(balances(A, "-10.00", B, "0.00", C, "10.00")))
                .containsExactly(transfer(A, C, "10.00"));
    }

    @Test
    void largestDebtorPaysFirst() {
        assertThat(DebtSimplifier.simplify(balances(A, "-10.00", B, "-20.00", C, "30.00")))
                .containsExactly(transfer(B, C, "20.00"), transfer(A, C, "10.00"));
    }

    @Test
    void matchesLargestDebtorWithLargestCreditor() {
        // C (-40) pays A (+50) 40; then D (-20) pays A the remaining 10 and B (+10) the other 10.
        assertThat(DebtSimplifier.simplify(balances(A, "50.00", B, "10.00", C, "-40.00", D, "-20.00")))
                .containsExactly(transfer(C, A, "40.00"), transfer(D, A, "10.00"), transfer(D, B, "10.00"));
    }

    @Test
    void sameInputAlwaysGivesSameTransfers() {
        Map<UUID, BigDecimal> balances = balances(A, "-5.00", B, "-5.00", C, "5.00", D, "5.00");

        assertThat(DebtSimplifier.simplify(balances)).isEqualTo(DebtSimplifier.simplify(new HashMap<>(balances)));
    }

    @Test
    void rejectsBalancesThatDoNotSumToZero() {
        assertThatThrownBy(() -> DebtSimplifier.simplify(balances(A, "10.00", B, "-9.99")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0.01");
    }

    /** The guarantees from the class comment, checked on many random groups. */
    @Test
    void settlesEveryoneWithAtMostNMinusOneTransfers() {
        Random random = new Random(7);

        for (int run = 0; run < 500; run++) {
            int people = 2 + random.nextInt(9);
            Map<UUID, BigDecimal> balances = randomBalancesSummingToZero(random, people);

            List<Transfer> transfers = DebtSimplifier.simplify(balances);

            long nonZero = balances.values().stream().filter(balance -> balance.signum() != 0).count();
            assertThat((long) transfers.size()).isLessThanOrEqualTo(Math.max(0, nonZero - 1));

            Map<UUID, BigDecimal> after = new HashMap<>(balances);
            for (Transfer transfer : transfers) {
                assertThat(transfer.amount()).isPositive();
                // Paying reduces what the payer owes and what the receiver is owed.
                after.merge(transfer.from(), transfer.amount(), BigDecimal::add);
                after.merge(transfer.to(), transfer.amount().negate(), BigDecimal::add);
            }
            assertThat(after.values()).allSatisfy(balance -> assertThat(balance).isZero());
        }
    }

    private static Map<UUID, BigDecimal> randomBalancesSummingToZero(Random random, int people) {
        List<Long> cents = new ArrayList<>();
        long sum = 0;
        for (int i = 0; i < people - 1; i++) {
            long value = random.nextInt(20_001) - 10_000; // -100.00 .. 100.00, zero included
            cents.add(value);
            sum += value;
        }
        cents.add(-sum); // the last person balances the group

        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        cents.forEach(value -> balances.put(UUID.randomUUID(), BigDecimal.valueOf(value, 2)));
        return balances;
    }
}
