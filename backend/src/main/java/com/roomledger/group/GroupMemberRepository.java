package com.roomledger.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    /** The membership in a group that has not been deleted. Every access check goes through this. */
    @Query("""
            select m from GroupMember m
            join fetch m.group g
            where g.id = :groupId and m.user.id = :userId and g.deletedAt is null
            """)
    Optional<GroupMember> findActiveMembership(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    /** Admins of active groups created before the given time, whose email matches a LIKE pattern. */
    @Query("""
            select m from GroupMember m
            join fetch m.group g
            join fetch m.user u
            where m.role = com.roomledger.group.GroupRole.ADMIN
              and g.deletedAt is null
              and g.createdAt < :before
              and u.email like :emailPattern
            """)
    List<GroupMember> findActiveAdminMembershipsByEmailCreatedBefore(@Param("emailPattern") String emailPattern,
                                                                     @Param("before") Instant before);

    @Query("select m.user.id from GroupMember m where m.group.id = :groupId")
    Set<UUID> findUserIdsByGroupId(@Param("groupId") UUID groupId);

    // "join fetch" loads the related entity in the same SQL query. Without it, reading
    // member.getGroup().getName() in a loop would run one extra query per member (the N+1 problem).
    @Query("""
            select m from GroupMember m
            join fetch m.group g
            where m.user.id = :userId and g.deletedAt is null
            order by m.joinedAt desc
            """)
    List<GroupMember> findAllByUserIdWithGroup(@Param("userId") UUID userId);

    @Query("""
            select m from GroupMember m
            join fetch m.user
            where m.group.id = :groupId
            order by m.joinedAt
            """)
    List<GroupMember> findAllByGroupIdWithUser(@Param("groupId") UUID groupId);
}
