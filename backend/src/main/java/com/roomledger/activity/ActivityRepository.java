package com.roomledger.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityRepository extends JpaRepository<ActivityEvent, UUID> {

    @EntityGraph(attributePaths = "actor")
    Page<ActivityEvent> findByGroupId(UUID groupId, Pageable pageable);
}
