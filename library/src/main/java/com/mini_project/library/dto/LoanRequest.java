package com.mini_project.library.dto;

import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotNull(message = "Book is required")
        Long bookId,

        @NotNull(message = "Member is required")
        Long memberId
) {
}
