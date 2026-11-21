package com.roomledger.settlement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The state machine alone, without Spring or a database (group and users are not needed for it). */
class SettlementTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    private static Settlement pending() {
        return Settlement.markedAsPaid(null, null, null, new BigDecimal("15.00"), null);
    }

    @Test
    void markedAsPaidStartsPendingAndUnresolved() {
        Settlement settlement = pending();

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.getResolvedAt()).isNull();
    }

    @Test
    void receivedDirectlyIsConfirmedImmediately() {
        Settlement settlement = Settlement.receivedDirectly(null, null, null, new BigDecimal("15.00"), "cash", NOW);

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(settlement.getResolvedAt()).isEqualTo(NOW);
    }

    @Test
    void pendingCanBeConfirmed() {
        Settlement settlement = pending();

        settlement.confirm(NOW);

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(settlement.getResolvedAt()).isEqualTo(NOW);
    }

    @Test
    void pendingCanBeRejected() {
        Settlement settlement = pending();

        settlement.reject(NOW);

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.REJECTED);
        assertThat(settlement.getResolvedAt()).isEqualTo(NOW);
    }

    @Test
    void confirmedIsFinal() {
        Settlement settlement = pending();
        settlement.confirm(NOW);

        assertThatThrownBy(() -> settlement.confirm(NOW)).isInstanceOf(InvalidSettlementTransitionException.class);
        assertThatThrownBy(() -> settlement.reject(NOW)).isInstanceOf(InvalidSettlementTransitionException.class);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
    }

    @Test
    void rejectedIsFinal() {
        Settlement settlement = pending();
        settlement.reject(NOW);

        assertThatThrownBy(() -> settlement.confirm(NOW)).isInstanceOf(InvalidSettlementTransitionException.class);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.REJECTED);
    }
}
