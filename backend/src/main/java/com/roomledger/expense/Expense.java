package com.roomledger.expense;

import com.roomledger.group.Group;
import com.roomledger.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ExpenseSource source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Set explicitly on every change, which also guarantees the entity is dirty,
    // so Hibernate always bumps the version (even when only shares/items changed).
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    /**
     * Optimistic locking: every UPDATE runs "... WHERE id = ? AND version = ?" and increments it.
     * If another transaction changed the row first, 0 rows match and Hibernate throws
     * an optimistic lock exception instead of silently overwriting the other change.
     */
    @Version
    private long version;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ExpenseItem> items = new ArrayList<>();

    public Expense(Group group, String description, BigDecimal totalAmount, User paidBy,
                   LocalDate expenseDate, SplitType splitType, ExpenseSource source, User createdBy) {
        this.group = group;
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy = paidBy;
        this.expenseDate = expenseDate;
        this.splitType = splitType;
        this.source = source;
        this.createdBy = createdBy;
        this.updatedAt = Instant.now();
    }

    public void update(String description, BigDecimal totalAmount, User paidBy, LocalDate expenseDate, SplitType splitType) {
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy = paidBy;
        this.expenseDate = expenseDate;
        this.splitType = splitType;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Updates existing share rows in place instead of clear() + add(). Hibernate flushes INSERTs
     * before DELETEs, so re-inserting a share for the same user would hit the
     * UNIQUE (expense_id, user_id) constraint before the old row is deleted.
     */
    public void replaceShares(Map<User, BigDecimal> newShares) {
        Map<UUID, BigDecimal> amountsByUserId = new HashMap<>();
        Map<UUID, User> usersById = new HashMap<>();
        newShares.forEach((user, amount) -> {
            amountsByUserId.put(user.getId(), amount);
            usersById.put(user.getId(), user);
        });

        shares.removeIf(share -> !amountsByUserId.containsKey(share.getUser().getId()));
        for (ExpenseShare share : shares) {
            UUID userId = share.getUser().getId();
            share.changeAmount(amountsByUserId.get(userId));
            usersById.remove(userId);
        }
        usersById.forEach((userId, user) -> shares.add(new ExpenseShare(this, user, amountsByUserId.get(userId))));
    }

    public void replaceItems(List<ExpenseItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }
}
