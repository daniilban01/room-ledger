package com.roomledger.expense;

import com.roomledger.activity.ActivityService;
import com.roomledger.activity.ActivityType;
import com.roomledger.common.PageResponse;
import com.roomledger.common.error.BadRequestException;
import com.roomledger.common.error.ConflictException;
import com.roomledger.common.error.ForbiddenException;
import com.roomledger.common.error.NotFoundException;
import com.roomledger.expense.dto.ExpenseRequest;
import com.roomledger.expense.dto.ExpenseResponse;
import com.roomledger.expense.dto.ExpenseSummaryResponse;
import com.roomledger.expense.dto.ItemRequest;
import com.roomledger.expense.dto.ShareRequest;
import com.roomledger.group.GroupAccess;
import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupMemberRepository;
import com.roomledger.user.User;
import com.roomledger.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("expenseDate"), Sort.Order.desc("createdAt"));

    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;
    private final ActivityService activityService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupMemberRepository memberRepository,
                          UserRepository userRepository,
                          GroupAccess groupAccess,
                          ActivityService activityService) {
        this.expenseRepository = expenseRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.groupAccess = groupAccess;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseSummaryResponse> list(UUID groupId, UUID userId, int page, int size) {
        groupAccess.requireMember(groupId, userId);
        var pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
        return PageResponse.from(
                expenseRepository.findByGroupIdAndDeletedFalse(groupId, pageRequest),
                ExpenseSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID groupId, UUID expenseId, UUID userId) {
        groupAccess.requireMember(groupId, userId);
        return ExpenseResponse.from(findExpense(groupId, expenseId));
    }

    @Transactional
    public ExpenseResponse create(UUID groupId, UUID userId, ExpenseRequest request) {
        GroupMember me = groupAccess.requireMember(groupId, userId);
        Map<UUID, BigDecimal> shares = calculateShares(groupId, request);

        Expense expense = new Expense(
                me.getGroup(),
                request.description().trim(),
                request.totalAmount(),
                userRef(request.paidBy()),
                request.expenseDate(),
                request.splitType(),
                request.source() == null ? ExpenseSource.MANUAL : request.source(),
                me.getUser());
        applySplit(expense, request, shares);

        // Flush so the INSERT runs now and generated values (createdAt) are in the response.
        expenseRepository.saveAndFlush(expense);
        activityService.record(me.getGroup(), me.getUser(), ActivityType.EXPENSE_ADDED, activityPayload(expense));
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse update(UUID groupId, UUID expenseId, UUID userId, ExpenseRequest request) {
        GroupMember me = groupAccess.requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        requireCanModify(me, expense);

        if (request.version() == null) {
            throw new BadRequestException("version is required when updating an expense");
        }
        // Catches the "lost update": the client edited a version that someone else has changed since.
        // @Version alone only catches two transactions overlapping in time on the server.
        if (request.version() != expense.getVersion()) {
            throw new ConflictException("The expense was modified by someone else. Reload it and try again.");
        }

        Map<UUID, BigDecimal> shares = calculateShares(groupId, request);
        expense.update(
                request.description().trim(),
                request.totalAmount(),
                userRef(request.paidBy()),
                request.expenseDate(),
                request.splitType());
        applySplit(expense, request, shares);

        // Flush now so the UPDATE (and its version check) runs here and the response carries the new version.
        expenseRepository.saveAndFlush(expense);
        activityService.record(me.getGroup(), me.getUser(), ActivityType.EXPENSE_EDITED, activityPayload(expense));
        return ExpenseResponse.from(expense);
    }

    /** Soft delete: the row stays (for the activity history) but disappears from lists and balances. */
    @Transactional
    public void delete(UUID groupId, UUID expenseId, UUID userId) {
        GroupMember me = groupAccess.requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        requireCanModify(me, expense);
        expense.markDeleted();
        activityService.record(me.getGroup(), me.getUser(), ActivityType.EXPENSE_DELETED, activityPayload(expense));
    }

    private static Map<String, Object> activityPayload(Expense expense) {
        return Map.of(
                "expenseId", expense.getId().toString(),
                "description", expense.getDescription(),
                // Stored as text: a JSON number could come back as a double and lose precision.
                "amount", expense.getTotalAmount().toPlainString());
    }

    private Expense findExpense(UUID groupId, UUID expenseId) {
        return expenseRepository.findByIdAndGroupIdAndDeletedFalse(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
    }

    private static void requireCanModify(GroupMember me, Expense expense) {
        boolean isCreator = expense.getCreatedBy().getId().equals(me.getUser().getId());
        if (!isCreator && !me.isAdmin()) {
            throw new ForbiddenException("Only the creator of the expense or a group admin can change it");
        }
    }

    /** Validates the request against the split type and group membership, then computes the shares. */
    private Map<UUID, BigDecimal> calculateShares(UUID groupId, ExpenseRequest request) {
        requireOnlyFieldsFor(request);

        Set<UUID> involved = new HashSet<>();
        involved.add(request.paidBy());
        involved.addAll(peopleIn(request));
        if (!memberRepository.findUserIdsByGroupId(groupId).containsAll(involved)) {
            throw new BadRequestException("The payer and all participants must be members of the group");
        }

        try {
            return switch (request.splitType()) {
                case EQUAL -> SplitCalculator.equal(request.totalAmount(), request.participants());
                case EXACT -> SplitCalculator.exact(request.totalAmount(), exactShares(request.shares()));
                case ITEMS -> SplitCalculator.items(request.totalAmount(), request.items().stream()
                        .map(item -> new SplitCalculator.Item(item.amount(), item.assignedTo()))
                        .toList());
            };
        } catch (InvalidSplitException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static void requireOnlyFieldsFor(ExpenseRequest request) {
        boolean hasParticipants = notEmpty(request.participants());
        boolean hasShares = notEmpty(request.shares());
        boolean hasItems = notEmpty(request.items());

        boolean valid = switch (request.splitType()) {
            case EQUAL -> hasParticipants && !hasShares && !hasItems;
            case EXACT -> hasShares && !hasParticipants && !hasItems;
            case ITEMS -> hasItems && !hasParticipants && !hasShares;
        };
        if (!valid) {
            throw new BadRequestException(switch (request.splitType()) {
                case EQUAL -> "An EQUAL split needs 'participants' (and no 'shares' or 'items')";
                case EXACT -> "An EXACT split needs 'shares' (and no 'participants' or 'items')";
                case ITEMS -> "An ITEMS split needs 'items' (and no 'participants' or 'shares')";
            });
        }
    }

    private static Map<UUID, BigDecimal> exactShares(List<ShareRequest> shares) {
        Map<UUID, BigDecimal> byUser = new LinkedHashMap<>();
        for (ShareRequest share : shares) {
            if (byUser.put(share.userId(), share.amount()) != null) {
                throw new BadRequestException("Each person can appear only once in 'shares'");
            }
        }
        return byUser;
    }

    private static Set<UUID> peopleIn(ExpenseRequest request) {
        return switch (request.splitType()) {
            case EQUAL -> new HashSet<>(request.participants());
            case EXACT -> request.shares().stream().map(ShareRequest::userId).collect(Collectors.toSet());
            case ITEMS -> request.items().stream()
                    .flatMap(item -> item.assignedTo().stream())
                    .collect(Collectors.toSet());
        };
    }

    private void applySplit(Expense expense, ExpenseRequest request, Map<UUID, BigDecimal> shares) {
        Map<User, BigDecimal> sharesByUser = new HashMap<>();
        shares.forEach((userId, amount) -> sharesByUser.put(userRef(userId), amount));
        expense.replaceShares(sharesByUser);

        List<ExpenseItem> items = new ArrayList<>();
        if (request.splitType() == SplitType.ITEMS) {
            for (int i = 0; i < request.items().size(); i++) {
                ItemRequest item = request.items().get(i);
                Set<User> assignees = new HashSet<>();
                item.assignedTo().forEach(id -> assignees.add(userRef(id)));
                items.add(new ExpenseItem(expense, item.name().trim(), item.amount(), i, assignees));
            }
        }
        expense.replaceItems(items);
    }

    // A reference (proxy) is enough to set a foreign key; membership was already checked.
    private User userRef(UUID userId) {
        return userRepository.getReferenceById(userId);
    }

    private static boolean notEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
}
