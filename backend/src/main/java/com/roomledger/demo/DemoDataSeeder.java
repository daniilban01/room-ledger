package com.roomledger.demo;

import com.roomledger.expense.ExpenseService;
import com.roomledger.expense.SplitType;
import com.roomledger.expense.dto.ExpenseRequest;
import com.roomledger.expense.dto.ItemRequest;
import com.roomledger.expense.dto.ShareRequest;
import com.roomledger.group.GroupMember;
import com.roomledger.group.GroupMemberRepository;
import com.roomledger.group.GroupService;
import com.roomledger.group.dto.CreateGroupRequest;
import com.roomledger.group.dto.GroupDetailsResponse;
import com.roomledger.settlement.SettlementService;
import com.roomledger.settlement.dto.CreateSettlementRequest;
import com.roomledger.user.User;
import com.roomledger.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Creates a private demo sandbox for every visitor: their own Alex (group admin) and three fictional
 * roommates, with expenses of every split type and payments in every state. Visitors never see or
 * change each other's data, and every sandbox starts from the same story.
 *
 * <p>Everything goes through the normal services, so the demo data passes the same validation as
 * real data and fills the activity log on its own. All demo accounts get random passwords nobody
 * knows: the only way in is POST /api/auth/demo.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoDataSeeder {

    static final String EMAIL_DOMAIN = "@demo.roomledger.app";
    static final String GROUP_NAME = "Apartament Mărăști";

    private final UserRepository userRepository;
    private final GroupMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final Clock clock;

    public DemoDataSeeder(UserRepository userRepository,
                          GroupMemberRepository memberRepository,
                          PasswordEncoder passwordEncoder,
                          GroupService groupService,
                          ExpenseService expenseService,
                          SettlementService settlementService,
                          Clock clock) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.settlementService = settlementService;
        this.clock = clock;
    }

    /** Creates a new sandbox and returns its Alex, the account the visitor is logged in as. */
    @Transactional
    public User createSandbox() {
        // Only makes the emails unique; visitors never see it.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User alex = newUser("alex", suffix, "Alex Demo");
        UUID a = alex.getId();
        UUID i = newUser("ioana", suffix, "Ioana Pop").getId();
        UUID m = newUser("mihai", suffix, "Mihai Ionescu").getId();
        UUID r = newUser("radu", suffix, "Radu Stan").getId();

        GroupDetailsResponse group = groupService.create(a, new CreateGroupRequest(GROUP_NAME));
        UUID g = group.id();
        for (UUID roommate : List.of(i, m, r)) {
            groupService.join(roommate, group.joinCode());
        }

        List<UUID> everyone = List.of(a, i, m, r);
        equal(g, a, "Chirie noiembrie", "3200.00", 20, everyone);
        equal(g, i, "Internet + TV", "60.00", 18, everyone);
        expenseService.create(g, m, new ExpenseRequest(
                "Kaufland", money("187.40"), m, daysAgo(12), SplitType.ITEMS, null, null,
                List.of(
                        item("Detergent rufe", "45.90", a, i, m, r),
                        item("Hârtie igienică", "32.50", a, i, m, r),
                        item("Cafea boabe", "54.00", a, i),
                        item("Lapte și ouă", "28.00", m, r),
                        item("Bere", "27.00", a, m, r)),
                null, null));
        expenseService.create(g, r, new ExpenseRequest(
                "Curent (Alex are aer condiționat)", money("248.60"), r, daysAgo(9), SplitType.EXACT, null,
                List.of(share(a, "74.60"), share(i, "58.00"), share(m, "58.00"), share(r, "58.00")),
                null, null, null));
        equal(g, i, "Pizza de vineri", "135.00", 5, List.of(a, i, m));
        equal(g, a, "Gaz", "180.00", 3, everyone);

        // One payment of each kind: confirmed, waiting for Alex's answer, and rejected.
        UUID fromRadu = pay(g, r, a, "400.00", "Partea mea de chirie, restul săptămâna viitoare");
        settlementService.confirm(g, fromRadu, a);
        pay(g, m, a, "800.00", "Chirie noiembrie");
        UUID fromIoana = pay(g, i, a, "300.00", "Revolut");
        settlementService.reject(g, fromIoana, a);

        return alex;
    }

    /**
     * Deletes (soft) every group administered by a demo account and created more than maxAge ago:
     * the seeded groups and any group a visitor created while trying the app. Returns how many.
     */
    @Transactional
    public int deleteSandboxesOlderThan(Duration maxAge) {
        List<GroupMember> expired = memberRepository.findActiveAdminMembershipsByEmailCreatedBefore(
                "%" + EMAIL_DOMAIN, clock.instant().minus(maxAge));
        expired.forEach(admin -> groupService.delete(admin.getGroup().getId(), admin.getUser().getId()));
        return expired.size();
    }

    private User newUser(String name, String suffix, String displayName) {
        return userRepository.save(new User(name + "-" + suffix + EMAIL_DOMAIN,
                passwordEncoder.encode(UUID.randomUUID().toString()), displayName));
    }

    private void equal(UUID groupId, UUID paidBy, String description, String total, int daysAgo,
                       List<UUID> participants) {
        expenseService.create(groupId, paidBy, new ExpenseRequest(
                description, money(total), paidBy, daysAgo(daysAgo), SplitType.EQUAL, participants,
                null, null, null, null));
    }

    private UUID pay(UUID groupId, UUID from, UUID to, String amount, String note) {
        // Created by the payer, so it starts as PENDING ("I paid"), like in the app.
        return settlementService.create(groupId, from,
                new CreateSettlementRequest(from, to, money(amount), note)).id();
    }

    private LocalDate daysAgo(int days) {
        return LocalDate.now(clock).minusDays(days);
    }

    private static ItemRequest item(String name, String amount, UUID... assignedTo) {
        return new ItemRequest(name, money(amount), Set.of(assignedTo));
    }

    private static ShareRequest share(UUID userId, String amount) {
        return new ShareRequest(userId, money(amount));
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
