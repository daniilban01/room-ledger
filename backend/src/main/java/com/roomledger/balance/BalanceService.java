package com.roomledger.balance;

import com.roomledger.balance.dto.GroupBalancesResponse;
import com.roomledger.balance.dto.MemberBalanceResponse;
import com.roomledger.balance.dto.SimplifiedDebtsResponse;
import com.roomledger.balance.dto.TransferResponse;
import com.roomledger.group.GroupAccess;
import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupMemberRepository;
import com.roomledger.user.User;
import com.roomledger.user.dto.UserSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Balances are computed on every request, never stored: a stored balance could drift out of sync
 * with the expenses it was derived from. The aggregate queries keep this cheap.
 */
@Service
public class BalanceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BalanceQueries balanceQueries;
    private final GroupMemberRepository memberRepository;
    private final GroupAccess groupAccess;

    public BalanceService(BalanceQueries balanceQueries, GroupMemberRepository memberRepository, GroupAccess groupAccess) {
        this.balanceQueries = balanceQueries;
        this.memberRepository = memberRepository;
        this.groupAccess = groupAccess;
    }

    @Transactional(readOnly = true)
    public GroupBalancesResponse getBalances(UUID groupId, UUID userId) {
        groupAccess.requireMember(groupId, userId);
        List<GroupMember> members = memberRepository.findAllByGroupIdWithUser(groupId);
        Map<UUID, BigDecimal> balances = calculateBalances(groupId, members);

        List<MemberBalanceResponse> memberBalances = members.stream()
                .map(member -> new MemberBalanceResponse(
                        member.getUser().getId(),
                        member.getUser().getDisplayName(),
                        balances.get(member.getUser().getId())))
                .toList();
        return new GroupBalancesResponse(balances.get(userId), memberBalances);
    }

    @Transactional(readOnly = true)
    public SimplifiedDebtsResponse getSimplifiedDebts(UUID groupId, UUID userId) {
        groupAccess.requireMember(groupId, userId);
        List<GroupMember> members = memberRepository.findAllByGroupIdWithUser(groupId);
        Map<UUID, BigDecimal> balances = calculateBalances(groupId, members);

        Map<UUID, UserSummaryResponse> usersById = members.stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toMap(User::getId, UserSummaryResponse::from));

        List<TransferResponse> transfers = DebtSimplifier.simplify(balances).stream()
                .map(transfer -> new TransferResponse(
                        usersById.get(transfer.from()), usersById.get(transfer.to()), transfer.amount()))
                .toList();
        return new SimplifiedDebtsResponse(countDirectDebts(groupId), transfers);
    }

    /**
     * How many payments would be needed without simplification: the number of pairs of people
     * where one still directly owes the other. Shown in the UI as "X direct payments -> N transfers".
     */
    private int countDirectDebts(UUID groupId) {
        // Net debt per unordered pair {a, b}, stored under the key "a:b" with a < b:
        // positive = a owes b, negative = b owes a.
        Map<String, BigDecimal> netByPair = new HashMap<>();
        balanceQueries.expenseDebtsPerPair(groupId)
                .forEach(debt -> addDebt(netByPair, debt.first(), debt.second(), debt.amount()));
        // A confirmed payment from A to B reduces what A owes B.
        balanceQueries.settlementsPerPair(groupId)
                .forEach(paid -> addDebt(netByPair, paid.first(), paid.second(), paid.amount().negate()));

        return (int) netByPair.values().stream().filter(net -> net.signum() != 0).count();
    }

    private static void addDebt(Map<String, BigDecimal> netByPair, UUID debtor, UUID creditor, BigDecimal amount) {
        String a = debtor.toString();
        String b = creditor.toString();
        if (a.compareTo(b) < 0) {
            netByPair.merge(a + ":" + b, amount, BigDecimal::add);
        } else {
            netByPair.merge(b + ":" + a, amount.negate(), BigDecimal::add);
        }
    }

    /**
     * balance = (sum of expenses they paid)
     *         - (sum of their expense shares)
     *         + (sum of CONFIRMED settlements they sent)
     *         - (sum of CONFIRMED settlements they received)
     * Sending money pays off debt (balance goes up); receiving it means you are owed less (goes down).
     * Every member gets an entry, including members with no activity (balance 0.00).
     */
    private Map<UUID, BigDecimal> calculateBalances(UUID groupId, List<GroupMember> members) {
        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        members.forEach(member -> balances.put(member.getUser().getId(), ZERO));

        balanceQueries.totalPaidPerUser(groupId)
                .forEach(paid -> balances.merge(paid.userId(), paid.amount(), BigDecimal::add));
        balanceQueries.totalOwedPerUser(groupId)
                .forEach(owed -> balances.merge(owed.userId(), owed.amount().negate(), BigDecimal::add));
        balanceQueries.totalSentPerUser(groupId)
                .forEach(sent -> balances.merge(sent.userId(), sent.amount(), BigDecimal::add));
        balanceQueries.totalReceivedPerUser(groupId)
                .forEach(received -> balances.merge(received.userId(), received.amount().negate(), BigDecimal::add));

        // Each expense's shares sum to its total and every settlement adds to one member what it takes
        // from another, so the group always nets to zero. If not, the data is corrupt: fail loudly
        // instead of showing balances that cannot be settled.
        BigDecimal sum = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.signum() != 0) {
            throw new IllegalStateException("Balances of group " + groupId + " sum to " + sum + " instead of 0");
        }
        return balances;
    }
}
