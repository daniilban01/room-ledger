package com.roomledger.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    /** Codes of deleted groups stop working. */
    Optional<Group> findByJoinCodeAndDeletedAtIsNull(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
