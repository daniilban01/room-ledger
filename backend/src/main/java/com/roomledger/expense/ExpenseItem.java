package com.roomledger.expense;

import com.roomledger.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One line of a receipt (only for ITEMS expenses), shared equally by the users assigned to it. */
@Entity
@Table(name = "expense_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false, updatable = false)
    private Expense expense;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Keeps the order the user entered (or the receipt listed) the items in.
    @Column(nullable = false)
    private int position;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "expense_item_assignees",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignedTo = new HashSet<>();

    public ExpenseItem(Expense expense, String name, BigDecimal amount, int position, Set<User> assignedTo) {
        this.expense = expense;
        this.name = name;
        this.amount = amount;
        this.position = position;
        this.assignedTo = new HashSet<>(assignedTo);
    }
}
