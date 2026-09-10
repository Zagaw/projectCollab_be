package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAttendeeResponse {
    private Long attendeeId;
    private Long userId;
    private String userName;
    private String response;
    private LocalDateTime respondedAt;
}
