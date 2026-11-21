package com.roomledger.expense;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, UUID> {

    /** The user's non-zero shares in expenses that someone else paid, newest first. */
    @Query("""
            select s from ExpenseShare s
            join fetch s.expense e
            join fetch e.paidBy
            where e.group.id = :groupId
              and e.deleted = false
              and s.user.id = :userId
              and e.paidBy.id <> :userId
              and s.amount > 0
            order by e.expenseDate desc, e.createdAt desc
            """)
    List<ExpenseShare> findOwedByUser(@Param("groupId") UUID groupId, @Param("userId") UUID userId, Pageable pageable);
}
