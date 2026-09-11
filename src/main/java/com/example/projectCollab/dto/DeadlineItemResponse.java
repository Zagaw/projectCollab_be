package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineItemResponse {
    private String entityType;
    private Long entityId;
    private String title;
    private LocalDateTime deadline;
    private boolean overdue;
    private Long teamId;
    private String teamName;
    private Long projectId;
    private String projectTitle;
}
