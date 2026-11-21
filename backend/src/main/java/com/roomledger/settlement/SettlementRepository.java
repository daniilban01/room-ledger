package com.roomledger.settlement;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    List<Settlement> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    @EntityGraph(attributePaths = {"fromUser", "toUser"})
    Optional<Settlement> findByIdAndGroupId(UUID id, UUID groupId);
}
