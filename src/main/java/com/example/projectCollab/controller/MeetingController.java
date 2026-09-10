package com.example.projectCollab.controller;

import com.example.projectCollab.dto.MeetingMinutesRequest;
import com.example.projectCollab.dto.MeetingRequest;
import com.example.projectCollab.dto.MeetingResponse;
import com.example.projectCollab.dto.MeetingRsvpRequest;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.MeetingService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final AuthUtil authUtil;

    @PostMapping("/api/teams/{teamId}/meetings")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'ADMIN')")
    public ResponseEntity<MeetingResponse> createMeeting(
            @PathVariable Long teamId,
            @Valid @RequestBody MeetingRequest request,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.createMeeting(teamId, request, currentUser));
    }

    @GetMapping("/api/teams/{teamId}/meetings")
    public ResponseEntity<List<MeetingResponse>> listTeamMeetings(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "all") String filter,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.listTeamMeetings(teamId, filter, currentUser));
    }

    @GetMapping("/api/meetings/my")
    public ResponseEntity<List<MeetingResponse>> getMyMeetings(
            @RequestParam(defaultValue = "all") String filter,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.getMyMeetings(filter, currentUser));
    }

    @GetMapping("/api/meetings/lecturer")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<MeetingResponse>> getLecturerMeetings(
            @RequestParam(defaultValue = "all") String filter,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.getLecturerMeetings(filter, currentUser));
    }

    @GetMapping("/api/meetings/{meetingId}")
    public ResponseEntity<MeetingResponse> getMeeting(
            @PathVariable Long meetingId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.getMeeting(meetingId, currentUser));
    }

    @PutMapping("/api/meetings/{meetingId}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'ADMIN')")
    public ResponseEntity<MeetingResponse> updateMeeting(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingRequest request,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, request, currentUser));
    }

    @PatchMapping("/api/meetings/{meetingId}/cancel")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'ADMIN')")
    public ResponseEntity<MeetingResponse> cancelMeeting(
            @PathVariable Long meetingId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.cancelMeeting(meetingId, currentUser));
    }

    @PatchMapping("/api/meetings/{meetingId}/minutes")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'ADMIN')")
    public ResponseEntity<MeetingResponse> saveMinutes(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingMinutesRequest request,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.saveMinutes(meetingId, request.getMinutes(), currentUser));
    }

    @PostMapping("/api/meetings/{meetingId}/rsvp")
    public ResponseEntity<MeetingResponse> rsvp(
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingRsvpRequest request,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(meetingService.rsvp(meetingId, request.getResponse(), currentUser));
    }
}
