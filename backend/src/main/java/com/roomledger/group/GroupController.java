package com.roomledger.group;

import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.group.dto.CreateGroupRequest;
import com.roomledger.group.dto.GroupDetailsResponse;
import com.roomledger.group.dto.GroupSummaryResponse;
import com.roomledger.group.dto.JoinGroupRequest;
import com.roomledger.group.dto.RenameGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupSummaryResponse> myGroups(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return groupService.listForUser(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetailsResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(currentUser.id(), request);
    }

    @GetMapping("/{groupId}")
    public GroupDetailsResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                    @PathVariable UUID groupId) {
        return groupService.get(groupId, currentUser.id());
    }

    @PatchMapping("/{groupId}")
    public GroupDetailsResponse rename(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable UUID groupId,
                                       @Valid @RequestBody RenameGroupRequest request) {
        return groupService.rename(groupId, currentUser.id(), request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID groupId) {
        groupService.delete(groupId, currentUser.id());
    }

    @PostMapping("/join")
    public GroupDetailsResponse join(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @Valid @RequestBody JoinGroupRequest request) {
        return groupService.join(currentUser.id(), request.joinCode());
    }

    @PostMapping("/{groupId}/join-code")
    public GroupDetailsResponse regenerateJoinCode(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable UUID groupId) {
        return groupService.regenerateJoinCode(groupId, currentUser.id());
    }
}
