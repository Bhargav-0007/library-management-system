package com.mini_project.library.controller;

import com.mini_project.library.dto.LoanRequest;
import com.mini_project.library.entity.Loan;
import com.mini_project.library.security.AuthUser;
import com.mini_project.library.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Loan borrow(@AuthenticationPrincipal AuthUser user, @RequestBody @Valid LoanRequest request) {
        return loanService.borrowBook(user.organizationId(), request.bookId(), request.memberId());
    }

    @PostMapping("/{id}/return")
    public Loan returnBook(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return loanService.returnBook(user.organizationId(), id);
    }

    /** {@code status} may be {@code all} (default), {@code active}, {@code overdue} or {@code returned}. */
    @GetMapping
    public List<Loan> getLoans(@AuthenticationPrincipal AuthUser user,
                               @RequestParam(required = false) String status) {
        return loanService.getLoans(user.organizationId(), status);
    }

    @GetMapping("/overdue")
    public List<Loan> getOverdueLoans(@AuthenticationPrincipal AuthUser user) {
        return loanService.getOverdueLoans(user.organizationId());
    }
}
