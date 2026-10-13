package com.mini_project.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotBlank(message = "ISBN is required")
        String isbn,

        @Min(value = 1, message = "A book needs at least 1 copy")
        @Max(value = 10000, message = "Copies cannot exceed 10000")
        Integer copies
) {
}
