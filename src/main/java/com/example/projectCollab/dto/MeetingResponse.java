package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private Long meetingId;
    private String title;
    private String agenda;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private String meetingLink;
    private String minutes;
    private String status;
    private Long teamId;
    private String teamName;
    private Long projectId;
    private String projectTitle;
    private Long createdBy;
    private String createdByName;
    @com.fasterxml.jackson.annotation.JsonProperty("canManage")
    private boolean canManage;
    private String myRsvp;
    private List<MeetingAttendeeResponse> attendees = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
