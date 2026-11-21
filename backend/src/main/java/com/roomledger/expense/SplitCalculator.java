package com.roomledger.expense;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Pure split logic: no Spring, no database, so it is fast and easy to unit test.
 *
 * All arithmetic is done in whole cents (long). Dividing BigDecimals directly would force us
 * to pick a rounding mode, and rounding each share separately can make the shares sum to
 * 99.99 or 100.01 instead of 100.00.
 *
 * Leftover cents use the largest-remainder method: after integer division every participant has
 * the same fractional remainder, so the tie is broken by a stable order and the first
 * {@code leftover} participants get one extra cent. 100.00 / 3 -> 33.34, 33.33, 33.33.
 */
public final class SplitCalculator {

    /**
     * Stable order used for leftover cents and for the output. Compares the UUID's text, not
     * {@link UUID#compareTo}: that one compares signed 64-bit halves, which gives a different order
     * than sorting the strings, and the frontend preview (which only has strings) must match exactly.
     */
    public static final Comparator<UUID> STABLE_ORDER = Comparator.comparing(UUID::toString);

    public record Item(BigDecimal amount, Set<UUID> assignees) {
    }

    private SplitCalculator() {
    }

    public static SortedMap<UUID, BigDecimal> equal(BigDecimal total, Collection<UUID> participants) {
        long totalCents = requirePositiveCents(total, "Total");
        if (participants == null || participants.isEmpty()) {
            throw new InvalidSplitException("At least one participant is required");
        }

        SortedMap<UUID, Long> cents = new TreeMap<>(STABLE_ORDER);
        distribute(totalCents, participants, cents);
        return toAmounts(cents);
    }

    public static SortedMap<UUID, BigDecimal> exact(BigDecimal total, Map<UUID, BigDecimal> shares) {
        long totalCents = requirePositiveCents(total, "Total");
        if (shares == null || shares.isEmpty()) {
            throw new InvalidSplitException("At least one share is required");
        }

        SortedMap<UUID, Long> cents = new TreeMap<>(STABLE_ORDER);
        long sum = 0;
        for (Map.Entry<UUID, BigDecimal> share : shares.entrySet()) {
            long shareCents = toCents(share.getValue(), "Share");
            if (shareCents < 0) {
                throw new InvalidSplitException("Shares cannot be negative");
            }
            cents.put(share.getKey(), shareCents);
            sum += shareCents;
        }

        if (sum != totalCents) {
            throw new InvalidSplitException(
                    "Shares must add up to the total %s, but they add up to %s".formatted(fromCents(totalCents), fromCents(sum)));
        }
        return toAmounts(cents);
    }

    /**
     * Each item is shared equally by its assignees, but rounding happens ONCE for the whole receipt,
     * not per item. Rounding per item gives every leftover cent to the same person (the first id),
     * so two people sharing 7.49 and 4.99 would pay 6.25 / 6.23 instead of 6.24 / 6.24.
     *
     * <p>To keep the exact fractions in integers, everything is scaled by the least common multiple
     * of the group sizes: with items shared by 2 and 3 people, scale = 6, and 1 cent shared by 3
     * becomes exactly 2 units each.
     */
    public static SortedMap<UUID, BigDecimal> items(BigDecimal total, List<Item> items) {
        long totalCents = requirePositiveCents(total, "Total");
        if (items == null || items.isEmpty()) {
            throw new InvalidSplitException("At least one item is required");
        }

        long scale = 1;
        long sum = 0;
        for (Item item : items) {
            long itemCents = requirePositiveCents(item.amount(), "Item amount");
            if (item.assignees() == null || item.assignees().isEmpty()) {
                throw new InvalidSplitException("Every item must be assigned to at least one person");
            }
            scale = lcm(scale, Set.copyOf(item.assignees()).size());
            sum += itemCents;
        }
        if (sum != totalCents) {
            throw new InvalidSplitException(
                    "Items must add up to the total %s, but they add up to %s".formatted(fromCents(totalCents), fromCents(sum)));
        }

        // Each person's exact share, in units of 1/scale cent.
        SortedMap<UUID, Long> exactShares = new TreeMap<>(STABLE_ORDER);
        for (Item item : items) {
            Set<UUID> people = Set.copyOf(item.assignees());
            // multiplyExact: fail loudly instead of overflowing silently (only possible with huge groups).
            long perPerson = Math.multiplyExact(item.amount().movePointRight(2).longValueExact(), scale / people.size());
            people.forEach(person -> exactShares.merge(person, perPerson, Long::sum));
        }
        return toAmounts(largestRemainder(exactShares, scale, totalCents));
    }

    /**
     * Rounds exact shares (in units of 1/scale cent) down to whole cents, then gives the cents that are
     * left to the people who lost the most in rounding; ties go by the stable order.
     */
    private static SortedMap<UUID, Long> largestRemainder(SortedMap<UUID, Long> exactShares, long scale, long totalCents) {
        SortedMap<UUID, Long> cents = new TreeMap<>(STABLE_ORDER);
        long assigned = 0;
        for (Map.Entry<UUID, Long> share : exactShares.entrySet()) {
            long whole = share.getValue() / scale;
            cents.put(share.getKey(), whole);
            assigned += whole;
        }

        List<UUID> largestRemainderFirst = exactShares.keySet().stream()
                .sorted(Comparator.comparingLong((UUID person) -> exactShares.get(person) % scale).reversed()
                        .thenComparing(STABLE_ORDER))
                .toList();
        // Always fewer leftover cents than people: each person's remainder is below one cent.
        for (int i = 0; i < totalCents - assigned; i++) {
            cents.merge(largestRemainderFirst.get(i), 1L, Long::sum);
        }
        return cents;
    }

    private static long lcm(long a, long b) {
        return a / gcd(a, b) * b;
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** Splits {@code amountCents} equally among the people and adds each part to {@code into}. */
    private static void distribute(long amountCents, Collection<UUID> people, SortedMap<UUID, Long> into) {
        TreeSet<UUID> sorted = new TreeSet<>(STABLE_ORDER); // also removes duplicates
        sorted.addAll(people);
        List<UUID> ordered = List.copyOf(sorted);
        long base = amountCents / ordered.size();
        long leftover = amountCents % ordered.size();

        for (int i = 0; i < ordered.size(); i++) {
            long part = base + (i < leftover ? 1 : 0);
            into.merge(ordered.get(i), part, Long::sum);
        }
    }

    private static long requirePositiveCents(BigDecimal amount, String label) {
        long cents = toCents(amount, label);
        if (cents <= 0) {
            throw new InvalidSplitException(label + " must be greater than 0");
        }
        return cents;
    }

    private static long toCents(BigDecimal amount, String label) {
        if (amount == null) {
            throw new InvalidSplitException(label + " is required");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidSplitException(label + " can have at most 2 decimals");
        }
        return amount.movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private static SortedMap<UUID, BigDecimal> toAmounts(SortedMap<UUID, Long> cents) {
        SortedMap<UUID, BigDecimal> amounts = new TreeMap<>(STABLE_ORDER);
        cents.forEach((user, value) -> amounts.put(user, fromCents(value)));
        return amounts;
    }
}
