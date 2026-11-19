package com.roomledger.balance;

import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.balance.dto.GroupBalancesResponse;
import com.roomledger.balance.dto.SimplifiedDebtsResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public GroupBalancesResponse balances(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                          @PathVariable UUID groupId) {
        return balanceService.getBalances(groupId, currentUser.id());
    }

    @GetMapping("/simplified")
    public SimplifiedDebtsResponse simplified(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @PathVariable UUID groupId) {
        return balanceService.getSimplifiedDebts(groupId, currentUser.id());
    }
}
