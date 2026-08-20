package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long taskId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOverdue;

    // Project info
    private Long projectId;
    private String projectTitle;

    // Team info
    private Long teamId;
    private String teamName;

    // Assigned user info
    private Long assignedTo;
    private String assignedToName;

    // Creator info
    private Long createdBy;
    private String createdByName;

    // Milestone info
    private Long milestoneId;
    private String milestoneTitle;
}