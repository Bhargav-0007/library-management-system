package com.mini_project.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email
) {
}