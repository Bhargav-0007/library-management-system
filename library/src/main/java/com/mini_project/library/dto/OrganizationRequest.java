package com.mini_project.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank(message = "Library name is required")
        @Size(max = 120, message = "Library name is too long")
        String name,

        @NotNull(message = "Loan duration is required")
        @Min(value = 1, message = "Loan duration must be at least 1 day")
        @Max(value = 365, message = "Loan duration cannot exceed 365 days")
        Integer loanDurationDays,

        @NotNull(message = "Loan limit is required")
        @Min(value = 1, message = "Loan limit must be at least 1")
        @Max(value = 100, message = "Loan limit cannot exceed 100")
        Integer maxActiveLoansPerMember
) {
}
