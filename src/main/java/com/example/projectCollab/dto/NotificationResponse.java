package com.example.projectCollab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String type;
    private String title;
    private String message;
    private String entityType;
    private Long entityId;
    private Long projectId;
    private Long actorId;
    private String actorName;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;
}
