package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long activityId;
    private String action;
    private String description;
    private String entityType;
    private Long entityId;
    private Long userId;
    private String userName;
    private Long projectId;
    private String projectTitle;
    private LocalDateTime createdAt;
}