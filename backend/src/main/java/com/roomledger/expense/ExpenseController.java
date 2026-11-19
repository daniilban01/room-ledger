package com.roomledger.expense;

import com.roomledger.auth.AuthenticatedUser;
import com.roomledger.common.PageResponse;
import com.roomledger.expense.dto.ExpenseRequest;
import com.roomledger.expense.dto.ExpenseResponse;
import com.roomledger.expense.dto.ExpenseSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public PageResponse<ExpenseSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable UUID groupId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return expenseService.list(groupId, currentUser.id(), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @PathVariable UUID groupId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(groupId, currentUser.id(), request);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser,
                               @PathVariable UUID groupId,
                               @PathVariable UUID expenseId) {
        return expenseService.get(groupId, expenseId, currentUser.id());
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                  @PathVariable UUID groupId,
                                  @PathVariable UUID expenseId,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(groupId, expenseId, currentUser.id(), request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                       @PathVariable UUID groupId,
                       @PathVariable UUID expenseId) {
        expenseService.delete(groupId, expenseId, currentUser.id());
    }
}
