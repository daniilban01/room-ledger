package com.roomledger.balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Turns net balances into a short list of transfers that settles the whole group.
 *
 * <p>Greedy algorithm: repeatedly take the largest debtor and the largest creditor (two max-heaps),
 * move min(debt, credit) from one to the other, and put whoever still has a non-zero balance back.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Every step brings at least one person to zero (the smaller side of the pair), and the last
 *       step brings two people to zero, so there are at most n - 1 transfers for n people with a
 *       non-zero balance.</li>
 *   <li>After applying the transfers every balance is exactly 0.00 (we work in whole cents).</li>
 * </ul>
 *
 * <p>Complexity: building the heaps is O(n log n); each step is O(log n) and there are at most
 * n - 1 steps, so O(n log n) overall, with O(n) extra memory.
 *
 * <p>Limitation: n - 1 is an upper bound, not always the true minimum. Finding the absolute minimum
 * number of transfers is NP-hard (it requires splitting people into the largest number of subgroups
 * whose balances sum to zero), so for a roommate app the greedy result is the practical choice.
 */
public final class DebtSimplifier {

    public record Transfer(UUID from, UUID to, BigDecimal amount) {
    }

    /** A person and how much they are owed (creditor) or owe (debtor), always as a positive number of cents. */
    private record Party(UUID userId, long cents) {
    }

    // Largest amount first; ties broken by id so the same balances always give the same transfers.
    private static final Comparator<Party> LARGEST_FIRST = Comparator
            .comparingLong(Party::cents).reversed()
            .thenComparing(party -> party.userId().toString());

    private DebtSimplifier() {
    }

    /** @param balances net balance per user: positive = is owed money, negative = owes money */
    public static List<Transfer> simplify(Map<UUID, BigDecimal> balances) {
        PriorityQueue<Party> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Party> debtors = new PriorityQueue<>(LARGEST_FIRST);

        long sum = 0;
        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            // longValueExact throws if a balance has more than 2 decimals: that would be a bug upstream.
            long cents = entry.getValue().movePointRight(2).longValueExact();
            sum += cents;
            if (cents > 0) {
                creditors.add(new Party(entry.getKey(), cents));
            } else if (cents < 0) {
                debtors.add(new Party(entry.getKey(), -cents));
            }
        }
        if (sum != 0) {
            throw new IllegalArgumentException("Balances must sum to zero, but they sum to " + BigDecimal.valueOf(sum, 2));
        }

        List<Transfer> transfers = new ArrayList<>();
        // Because the balances sum to zero, debtors and creditors run out at the same time.
        while (!debtors.isEmpty()) {
            Party debtor = debtors.poll();
            Party creditor = creditors.poll();
            long amount = Math.min(debtor.cents(), creditor.cents());

            transfers.add(new Transfer(debtor.userId(), creditor.userId(), BigDecimal.valueOf(amount, 2)));

            if (debtor.cents() > amount) {
                debtors.add(new Party(debtor.userId(), debtor.cents() - amount));
            }
            if (creditor.cents() > amount) {
                creditors.add(new Party(creditor.userId(), creditor.cents() - amount));
            }
        }
        return transfers;
    }
}
