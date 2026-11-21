package com.roomledger.settlement;

import com.roomledger.group.Group;
import com.roomledger.user.User;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A payment from one member to another, recorded in the ledger (the app never moves money).
 *
 * <p>State machine:
 * <pre>
 *   PENDING --confirm--> CONFIRMED   (only CONFIRMED settlements change balances)
 *   PENDING --reject---> REJECTED
 * </pre>
 * CONFIRMED and REJECTED are final. The transition rules live here, in the entity, so no code
 * path can put a settlement into an invalid state; who may trigger them is checked in the service.
 */
@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user", nullable = false, updatable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user", nullable = false, updatable = false)
    private User toUser;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    @Column(updatable = false)
    private String note;

    // Two people acting on the same settlement at once: only the first write wins, the other gets 409.
    @Version
    private long version;

    private Settlement(Group group, User fromUser, User toUser, BigDecimal amount, String note,
                       SettlementStatus status, User createdBy, Instant resolvedAt) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.note = note;
        this.status = status;
        this.createdBy = createdBy;
        this.resolvedAt = resolvedAt;
    }

    /** The payer says "I paid": waits for the receiver to confirm. */
    public static Settlement markedAsPaid(Group group, User from, User to, BigDecimal amount, String note) {
        return new Settlement(group, from, to, amount, note, SettlementStatus.PENDING, from, null);
    }

    /** The receiver says "I received it": nothing to confirm, it is final immediately. */
    public static Settlement receivedDirectly(Group group, User from, User to, BigDecimal amount, String note,
                                              Instant now) {
        return new Settlement(group, from, to, amount, note, SettlementStatus.CONFIRMED, to, now);
    }

    public void confirm(Instant now) {
        transitionTo(SettlementStatus.CONFIRMED, now);
    }

    public void reject(Instant now) {
        transitionTo(SettlementStatus.REJECTED, now);
    }

    private void transitionTo(SettlementStatus target, Instant now) {
        if (status != SettlementStatus.PENDING) {
            throw new InvalidSettlementTransitionException(status, target);
        }
        this.status = target;
        this.resolvedAt = now;
    }
}
