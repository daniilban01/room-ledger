package com.roomledger.settlement;

import com.roomledger.activity.ActivityService;
import com.roomledger.activity.ActivityType;
import com.roomledger.common.error.BadRequestException;
import com.roomledger.common.error.ConflictException;
import com.roomledger.common.error.ForbiddenException;
import com.roomledger.common.error.NotFoundException;
import com.roomledger.expense.ExpenseShareRepository;
import com.roomledger.group.GroupAccess;
import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupMemberRepository;
import com.roomledger.settlement.dto.CreateSettlementRequest;
import com.roomledger.settlement.dto.SettlementDetailsResponse;
import com.roomledger.settlement.dto.SettlementResponse;
import com.roomledger.user.User;
import com.roomledger.user.UserRepository;
import com.roomledger.user.dto.UserSummaryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SettlementService {

    private static final int MAX_CONTEXT_EXPENSES = 20;

    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;
    private final ActivityService activityService;
    private final ExpenseShareRepository expenseShareRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             GroupMemberRepository memberRepository,
                             UserRepository userRepository,
                             GroupAccess groupAccess,
                             ActivityService activityService,
                             ExpenseShareRepository expenseShareRepository) {
        this.settlementRepository = settlementRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.groupAccess = groupAccess;
        this.activityService = activityService;
        this.expenseShareRepository = expenseShareRepository;
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> list(UUID groupId, UUID userId) {
        groupAccess.requireMember(groupId, userId);
        return settlementRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(SettlementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementDetailsResponse get(UUID groupId, UUID settlementId, UUID userId) {
        groupAccess.requireMember(groupId, userId);
        Settlement settlement = findSettlement(groupId, settlementId);

        List<SettlementDetailsResponse.OwedExpense> owed = expenseShareRepository
                .findOwedByUser(groupId, settlement.getFromUser().getId(), PageRequest.of(0, MAX_CONTEXT_EXPENSES))
                .stream()
                .map(share -> new SettlementDetailsResponse.OwedExpense(
                        share.getExpense().getId(),
                        share.getExpense().getDescription(),
                        share.getExpense().getExpenseDate(),
                        UserSummaryResponse.from(share.getExpense().getPaidBy()),
                        share.getAmount()))
                .toList();
        return new SettlementDetailsResponse(SettlementResponse.from(settlement), owed);
    }

    /**
     * The payer creates a PENDING settlement ("I paid"); the receiver creates a CONFIRMED one
     * ("I received it"). Nobody can record a payment between two other people.
     */
    @Transactional
    public SettlementResponse create(UUID groupId, UUID userId, CreateSettlementRequest request) {
        GroupMember me = groupAccess.requireMember(groupId, userId);

        if (request.fromUserId().equals(request.toUserId())) {
            throw new BadRequestException("A payment needs two different people");
        }
        Set<UUID> memberIds = memberRepository.findUserIdsByGroupId(groupId);
        if (!memberIds.contains(request.fromUserId()) || !memberIds.contains(request.toUserId())) {
            throw new BadRequestException("Both people must be members of the group");
        }

        User from = userRepository.getReferenceById(request.fromUserId());
        User to = userRepository.getReferenceById(request.toUserId());
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        Settlement settlement;
        ActivityType activityType;
        if (userId.equals(request.fromUserId())) {
            settlement = Settlement.markedAsPaid(me.getGroup(), from, to, request.amount(), note);
            activityType = ActivityType.SETTLEMENT_MARKED_PAID;
        } else if (userId.equals(request.toUserId())) {
            settlement = Settlement.receivedDirectly(me.getGroup(), from, to, request.amount(), note, Instant.now());
            activityType = ActivityType.SETTLEMENT_RECORDED;
        } else {
            throw new ForbiddenException("You can only record payments you made or received");
        }

        settlementRepository.saveAndFlush(settlement);
        activityService.record(me.getGroup(), me.getUser(), activityType, payload(settlement));
        return SettlementResponse.from(settlement);
    }

    @Transactional
    public SettlementResponse confirm(UUID groupId, UUID settlementId, UUID userId) {
        return resolve(groupId, settlementId, userId, true);
    }

    @Transactional
    public SettlementResponse reject(UUID groupId, UUID settlementId, UUID userId) {
        return resolve(groupId, settlementId, userId, false);
    }

    private SettlementResponse resolve(UUID groupId, UUID settlementId, UUID userId, boolean confirm) {
        GroupMember me = groupAccess.requireMember(groupId, userId);
        Settlement settlement = findSettlement(groupId, settlementId);

        // Only the receiver knows whether the money actually arrived.
        if (!settlement.getToUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the person receiving the payment can confirm or reject it");
        }

        try {
            if (confirm) {
                settlement.confirm(Instant.now());
            } else {
                settlement.reject(Instant.now());
            }
        } catch (InvalidSettlementTransitionException e) {
            throw new ConflictException(e.getMessage());
        }

        // Flush here: if someone else resolved it in parallel, the @Version check fails now (-> 409).
        settlementRepository.saveAndFlush(settlement);
        activityService.record(me.getGroup(), me.getUser(),
                confirm ? ActivityType.SETTLEMENT_CONFIRMED : ActivityType.SETTLEMENT_REJECTED,
                payload(settlement));
        return SettlementResponse.from(settlement);
    }

    private Settlement findSettlement(UUID groupId, UUID settlementId) {
        return settlementRepository.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> new NotFoundException("Settlement not found"));
    }

    private static Map<String, Object> payload(Settlement settlement) {
        return Map.of(
                "settlementId", settlement.getId().toString(),
                "fromUserId", settlement.getFromUser().getId().toString(),
                "fromName", settlement.getFromUser().getDisplayName(),
                "toUserId", settlement.getToUser().getId().toString(),
                "toName", settlement.getToUser().getDisplayName(),
                // Stored as text: a JSON number could come back as a double and lose precision.
                "amount", settlement.getAmount().toPlainString());
    }
}
