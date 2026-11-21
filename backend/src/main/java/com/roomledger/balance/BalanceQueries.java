package com.roomledger.balance;

import com.roomledger.settlement.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Aggregate queries for balances. The database does the summing (SUM ... GROUP BY) and returns
 * one row per user, instead of us loading every expense and share into memory and adding them in Java.
 * The cost stays the same whether a group has 10 expenses or 10,000.
 */
@Repository
public class BalanceQueries {

    private final EntityManager entityManager;

    public BalanceQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** How much each member paid, over all non-deleted expenses of the group. */
    public List<UserAmount> totalPaidPerUser(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.UserAmount(e.paidBy.id, sum(e.totalAmount))
                        from Expense e
                        where e.group.id = :groupId and e.deleted = false
                        group by e.paidBy.id
                        """, UserAmount.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    /** How much each member paid to others through CONFIRMED settlements (pending/rejected ones do not count). */
    public List<UserAmount> totalSentPerUser(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.UserAmount(s.fromUser.id, sum(s.amount))
                        from Settlement s
                        where s.group.id = :groupId and s.status = :status
                        group by s.fromUser.id
                        """, UserAmount.class)
                .setParameter("groupId", groupId)
                .setParameter("status", SettlementStatus.CONFIRMED)
                .getResultList();
    }

    /** How much each member received from others through CONFIRMED settlements. */
    public List<UserAmount> totalReceivedPerUser(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.UserAmount(s.toUser.id, sum(s.amount))
                        from Settlement s
                        where s.group.id = :groupId and s.status = :status
                        group by s.toUser.id
                        """, UserAmount.class)
                .setParameter("groupId", groupId)
                .setParameter("status", SettlementStatus.CONFIRMED)
                .getResultList();
    }

    /**
     * Raw "who owes whom" before simplification: for every (debtor, creditor) pair, how much the
     * debtor's shares in expenses paid by the creditor add up to.
     */
    public List<PairAmount> expenseDebtsPerPair(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.PairAmount(s.user.id, e.paidBy.id, sum(s.amount))
                        from ExpenseShare s join s.expense e
                        where e.group.id = :groupId and e.deleted = false and s.user.id <> e.paidBy.id
                        group by s.user.id, e.paidBy.id
                        """, PairAmount.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    /** CONFIRMED payments per (payer, receiver) pair. */
    public List<PairAmount> settlementsPerPair(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.PairAmount(s.fromUser.id, s.toUser.id, sum(s.amount))
                        from Settlement s
                        where s.group.id = :groupId and s.status = :status
                        group by s.fromUser.id, s.toUser.id
                        """, PairAmount.class)
                .setParameter("groupId", groupId)
                .setParameter("status", SettlementStatus.CONFIRMED)
                .getResultList();
    }

    /** How much each member owes (sum of their shares), over all non-deleted expenses of the group. */
    public List<UserAmount> totalOwedPerUser(UUID groupId) {
        return entityManager.createQuery("""
                        select new com.roomledger.balance.UserAmount(s.user.id, sum(s.amount))
                        from ExpenseShare s
                        where s.expense.group.id = :groupId and s.expense.deleted = false
                        group by s.user.id
                        """, UserAmount.class)
                .setParameter("groupId", groupId)
                .getResultList();
    }
}
