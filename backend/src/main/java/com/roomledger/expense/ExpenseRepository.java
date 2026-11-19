package com.roomledger.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Filtering by group too means an id from another group is simply "not found".
    Optional<Expense> findByIdAndGroupIdAndDeletedFalse(UUID id, UUID groupId);

    // The list shows who paid, so load paidBy in the same query (avoids one query per row).
    @EntityGraph(attributePaths = "paidBy")
    Page<Expense> findByGroupIdAndDeletedFalse(UUID groupId, Pageable pageable);
}
