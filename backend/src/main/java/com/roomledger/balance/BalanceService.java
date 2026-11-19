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
        return new SimplifiedDebtsResponse(transfers);
    }

    /**
     * balance = (sum of expenses they paid) - (sum of their expense shares).
     * Every member gets an entry, including members with no expenses (balance 0.00).
     */
    private Map<UUID, BigDecimal> calculateBalances(UUID groupId, List<GroupMember> members) {
        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        members.forEach(member -> balances.put(member.getUser().getId(), ZERO));

        balanceQueries.totalPaidPerUser(groupId)
                .forEach(paid -> balances.merge(paid.userId(), paid.amount(), BigDecimal::add));
        balanceQueries.totalOwedPerUser(groupId)
                .forEach(owed -> balances.merge(owed.userId(), owed.amount().negate(), BigDecimal::add));

        // Each expense's shares sum to its total, so the group always nets to zero. If not, the data
        // is corrupt: fail loudly instead of showing balances that cannot be settled.
        BigDecimal sum = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.signum() != 0) {
            throw new IllegalStateException("Balances of group " + groupId + " sum to " + sum + " instead of 0");
        }
        return balances;
    }
}
