package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {
    private Long milestoneId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long teamId;
    private String teamName;
    private Long createdBy;
    private String createdByName;
    private String projectTitle;
    private Long projectId;
}