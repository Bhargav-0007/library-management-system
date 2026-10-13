package com.mini_project.library.dto;

import com.mini_project.library.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffRequest(
        @NotBlank(message = "Name is required")
        String fullName,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Temporary password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role
) {
}
