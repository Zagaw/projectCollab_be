package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProjectRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        LocalDateTime endDate,

        @NotBlank(message = "Course is required")
        @Size(max = 100, message = "Course cannot exceed 100 characters")
        String course,

        @NotBlank(message = "Semester is required")
        @Size(max = 20, message = "Semester cannot exceed 20 characters")
        String semester
) {
}