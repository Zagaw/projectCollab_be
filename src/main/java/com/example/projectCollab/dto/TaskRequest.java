package com.example.projectCollab.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    private String status = "TODO";

    private String priority = "MEDIUM";

    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long teamId;

    private Long assignedTo;

    private Long milestoneId;
}