package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRequest(
        @NotBlank(message = "Team name is required")
        @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Project ID is required")
        Long projectId
) {
}
