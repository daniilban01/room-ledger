package com.roomledger.settlement;

import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.settlement.dto.CreateSettlementRequest;
import com.roomledger.settlement.dto.SettlementDetailsResponse;
import com.roomledger.settlement.dto.SettlementResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public List<SettlementResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @PathVariable UUID groupId) {
        return settlementService.list(groupId, currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @PathVariable UUID groupId,
                                     @Valid @RequestBody CreateSettlementRequest request) {
        return settlementService.create(groupId, currentUser.id(), request);
    }

    @GetMapping("/{settlementId}")
    public SettlementDetailsResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @PathVariable UUID groupId,
                                         @PathVariable UUID settlementId) {
        return settlementService.get(groupId, settlementId, currentUser.id());
    }

    // POST, not PUT/PATCH: these are actions with rules (who, from which state), not field updates.
    @PostMapping("/{settlementId}/confirm")
    public SettlementResponse confirm(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                      @PathVariable UUID groupId,
                                      @PathVariable UUID settlementId) {
        return settlementService.confirm(groupId, settlementId, currentUser.id());
    }

    @PostMapping("/{settlementId}/reject")
    public SettlementResponse reject(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                     @PathVariable UUID groupId,
                                     @PathVariable UUID settlementId) {
        return settlementService.reject(groupId, settlementId, currentUser.id());
    }
}
