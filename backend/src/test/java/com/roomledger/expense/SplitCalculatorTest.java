package com.roomledger.expense;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitCalculatorTest {

    // Fixed ids so the "stable order" is predictable: A < B < C as strings.
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000000");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000000");
    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000000");

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal sum(Map<UUID, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Nested
    class Equal {

        @Test
        void splitsEvenAmountsEvenly() {
            assertThat(SplitCalculator.equal(money("90.00"), List.of(A, B, C)))
                    .containsExactly(Map.entry(A, money("30.00")), Map.entry(B, money("30.00")), Map.entry(C, money("30.00")));
        }

        @Test
        void givesLeftoverCentToFirstInStableOrder() {
            // Input order must not matter: C, A, B in, A, B, C out.
            assertThat(SplitCalculator.equal(money("100.00"), List.of(C, A, B)))
                    .containsExactly(Map.entry(A, money("33.34")), Map.entry(B, money("33.33")), Map.entry(C, money("33.33")));
        }

        @Test
        void distributesTwoLeftoverCents() {
            assertThat(SplitCalculator.equal(money("0.05"), List.of(A, B, C)))
                    .containsExactly(Map.entry(A, money("0.02")), Map.entry(B, money("0.02")), Map.entry(C, money("0.01")));
        }

        @Test
        void oneCentAmongThreeLeavesOthersAtZero() {
            assertThat(SplitCalculator.equal(money("0.01"), List.of(A, B, C)))
                    .containsExactly(Map.entry(A, money("0.01")), Map.entry(B, money("0.00")), Map.entry(C, money("0.00")));
        }

        @Test
        void ignoresDuplicateParticipants() {
            assertThat(SplitCalculator.equal(money("10.00"), List.of(A, A, B)))
                    .containsExactly(Map.entry(A, money("5.00")), Map.entry(B, money("5.00")));
        }

        @Test
        void acceptsAmountsWithFewerDecimals() {
            assertThat(SplitCalculator.equal(money("10"), List.of(A, B)))
                    .containsExactly(Map.entry(A, money("5.00")), Map.entry(B, money("5.00")));
        }

        @Test
        void usesStringOrderNotUuidCompareTo() {
            // UUID.compareTo compares signed longs, so it sorts "8..." BEFORE "1..." (negative high bits).
            // We sort the text, like the frontend does, so "1..." comes first and gets the extra cent.
            UUID high = UUID.fromString("80000000-0000-0000-0000-000000000000");
            UUID low = UUID.fromString("10000000-0000-0000-0000-000000000000");
            assertThat(high.compareTo(low)).isNegative();

            assertThat(SplitCalculator.equal(money("0.01"), List.of(high, low)))
                    .containsEntry(low, money("0.01"))
                    .containsEntry(high, money("0.00"));
        }

        @Test
        void rejectsInvalidInput() {
            assertThatThrownBy(() -> SplitCalculator.equal(money("0.00"), List.of(A)))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("greater than 0");
            assertThatThrownBy(() -> SplitCalculator.equal(money("-5.00"), List.of(A)))
                    .isInstanceOf(InvalidSplitException.class);
            assertThatThrownBy(() -> SplitCalculator.equal(money("10.001"), List.of(A)))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("2 decimals");
            assertThatThrownBy(() -> SplitCalculator.equal(money("10.00"), List.of()))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("participant");
            assertThatThrownBy(() -> SplitCalculator.equal(null, List.of(A)))
                    .isInstanceOf(InvalidSplitException.class);
        }
    }

    @Nested
    class Exact {

        @Test
        void keepsAmountsThatAddUpToTotal() {
            Map<UUID, BigDecimal> shares = Map.of(A, money("70.50"), B, money("29.50"));

            assertThat(SplitCalculator.exact(money("100.00"), shares))
                    .containsExactly(Map.entry(A, money("70.50")), Map.entry(B, money("29.50")));
        }

        @Test
        void allowsZeroShare() {
            assertThat(SplitCalculator.exact(money("10.00"), Map.of(A, money("10.00"), B, money("0"))))
                    .containsEntry(B, money("0.00"));
        }

        @Test
        void rejectsSharesThatDoNotAddUp() {
            Map<UUID, BigDecimal> shares = Map.of(A, money("50.00"), B, money("40.00"));

            assertThatThrownBy(() -> SplitCalculator.exact(money("100.00"), shares))
                    .isInstanceOf(InvalidSplitException.class)
                    .hasMessageContaining("100.00")
                    .hasMessageContaining("90.00");
        }

        @Test
        void rejectsNegativeShare() {
            Map<UUID, BigDecimal> shares = Map.of(A, money("110.00"), B, money("-10.00"));

            assertThatThrownBy(() -> SplitCalculator.exact(money("100.00"), shares))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("negative");
        }

        @Test
        void rejectsEmptyShares() {
            assertThatThrownBy(() -> SplitCalculator.exact(money("10.00"), Map.of()))
                    .isInstanceOf(InvalidSplitException.class);
        }
    }

    @Nested
    class Items {

        @Test
        void sumsEachPersonsPartOfTheirItems() {
            List<SplitCalculator.Item> items = List.of(
                    new SplitCalculator.Item(money("10.00"), Set.of(A, B)),   // 5.00 each
                    new SplitCalculator.Item(money("4.50"), Set.of(A)),       // A only
                    new SplitCalculator.Item(money("3.00"), Set.of(B, C)));   // 1.50 each

            assertThat(SplitCalculator.items(money("17.50"), items))
                    .containsExactly(Map.entry(A, money("9.50")), Map.entry(B, money("6.50")), Map.entry(C, money("1.50")));
        }

        @Test
        void splitsEachItemWithTheRemainderRule() {
            List<SplitCalculator.Item> items = List.of(new SplitCalculator.Item(money("1.00"), Set.of(A, B, C)));

            assertThat(SplitCalculator.items(money("1.00"), items))
                    .containsExactly(Map.entry(A, money("0.34")), Map.entry(B, money("0.33")), Map.entry(C, money("0.33")));
        }

        @Test
        void roundsOnceForTheWholeReceiptSoLeftoverCentsDoNotPileUpOnOnePerson() {
            // Two odd-priced items shared by two people. Rounding per item would give A both
            // extra cents (18.66 / 18.64); the receipt total 37.30 splits evenly.
            List<SplitCalculator.Item> items = List.of(
                    new SplitCalculator.Item(money("7.49"), Set.of(A, B)),
                    new SplitCalculator.Item(money("4.99"), Set.of(A, B)),
                    new SplitCalculator.Item(money("18.50"), Set.of(A, B)),
                    new SplitCalculator.Item(money("6.32"), Set.of(A, B)));

            assertThat(SplitCalculator.items(money("37.30"), items))
                    .containsExactly(Map.entry(A, money("18.65")), Map.entry(B, money("18.65")));
        }

        @Test
        void leftoverCentGoesToWhoeverLostMostInRounding() {
            // A's exact share is 0.50 + 0.3333 = 0.8333; B's is 0.50 + 0.3333; C's is 0.3333.
            // Whole cents: 83 + 83 + 33 = 199, one cent left. All remainders are equal (1/3), so the tie
            // goes to A by id. With different remainders, the largest one would win regardless of id.
            List<SplitCalculator.Item> items = List.of(
                    new SplitCalculator.Item(money("1.00"), Set.of(A, B)),
                    new SplitCalculator.Item(money("1.00"), Set.of(A, B, C)));

            assertThat(SplitCalculator.items(money("2.00"), items))
                    .containsExactly(Map.entry(A, money("0.84")), Map.entry(B, money("0.83")), Map.entry(C, money("0.33")));
        }

        @Test
        void rejectsItemsThatDoNotAddUpToTotal() {
            List<SplitCalculator.Item> items = List.of(new SplitCalculator.Item(money("5.00"), Set.of(A)));

            assertThatThrownBy(() -> SplitCalculator.items(money("6.00"), items))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("add up");
        }

        @Test
        void rejectsItemWithoutAssignees() {
            List<SplitCalculator.Item> items = List.of(new SplitCalculator.Item(money("5.00"), Set.of()));

            assertThatThrownBy(() -> SplitCalculator.items(money("5.00"), items))
                    .isInstanceOf(InvalidSplitException.class).hasMessageContaining("assigned");
        }

        @Test
        void rejectsZeroPricedItem() {
            List<SplitCalculator.Item> items = List.of(
                    new SplitCalculator.Item(money("5.00"), Set.of(A)),
                    new SplitCalculator.Item(money("0.00"), Set.of(A)));

            assertThatThrownBy(() -> SplitCalculator.items(money("5.00"), items))
                    .isInstanceOf(InvalidSplitException.class);
        }
    }

    /** The core invariant, checked on many random inputs: shares always sum exactly to the total. */
    @Test
    void sharesAlwaysSumToTotal() {
        Random random = new Random(42); // fixed seed: random but reproducible
        List<UUID> people = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            people.add(UUID.randomUUID());
        }

        for (int run = 0; run < 500; run++) {
            BigDecimal total = BigDecimal.valueOf(1 + random.nextInt(1_000_000), 2);
            List<UUID> participants = people.subList(0, 1 + random.nextInt(people.size()));

            SortedMap<UUID, BigDecimal> equal = SplitCalculator.equal(total, participants);
            assertThat(sum(equal)).isEqualByComparingTo(total);
            // No share differs from another by more than one cent.
            BigDecimal max = equal.values().stream().max(BigDecimal::compareTo).orElseThrow();
            BigDecimal min = equal.values().stream().min(BigDecimal::compareTo).orElseThrow();
            assertThat(max.subtract(min)).isLessThanOrEqualTo(money("0.01"));

            List<SplitCalculator.Item> items = new ArrayList<>();
            long remaining = total.movePointRight(2).longValueExact();
            while (remaining > 0) {
                long itemCents = Math.min(remaining, 1 + random.nextInt(5_000));
                remaining -= itemCents;
                Set<UUID> assignees = new HashSet<>(participants.subList(0, 1 + random.nextInt(participants.size())));
                items.add(new SplitCalculator.Item(BigDecimal.valueOf(itemCents, 2), assignees));
            }
            assertThat(sum(SplitCalculator.items(total, items))).isEqualByComparingTo(total);

            Map<UUID, BigDecimal> exact = new LinkedHashMap<>(equal);
            assertThat(sum(SplitCalculator.exact(total, exact))).isEqualByComparingTo(total);
        }
    }
}
