package com.mini_project.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Library name is required")
        @Size(max = 120, message = "Library name is too long")
        String organizationName,

        @NotBlank(message = "Your name is required")
        String fullName,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password
) {
}
