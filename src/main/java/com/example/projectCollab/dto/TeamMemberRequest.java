package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotNull;

public record TeamMemberRequest(
        @NotNull(message = "Team ID is required")
        Long teamId,

        @NotNull(message = "User ID is required")
        Long userId
) {
}