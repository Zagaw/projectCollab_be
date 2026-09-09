package com.example.projectCollab.controller;

import com.example.projectCollab.dto.InvitationResponse;
import com.example.projectCollab.dto.TeamMemberRequest;
import com.example.projectCollab.dto.TeamResponse;
import com.example.projectCollab.service.TeamMemberService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;
    private final AuthUtil authUtil;

    public TeamMemberController(TeamMemberService teamMemberService, AuthUtil authUtil) {
        this.teamMemberService = teamMemberService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // INVITE STUDENT (Lecturer, Admin, or Team Leader of that team)
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<InvitationResponse> inviteStudent(
            @Valid @RequestBody TeamMemberRequest request,
            Authentication authentication) {

        Long actorId = authUtil.getCurrentUserId(authentication);
        InvitationResponse response = teamMemberService.inviteStudent(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // ACCEPT INVITATION (Student only)
    // ==========================================

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<TeamResponse> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        TeamResponse response = teamMemberService.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // REJECT INVITATION (Student only)
    // ==========================================

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        teamMemberService.rejectInvitation(invitationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // GET MY PENDING INVITATIONS
    // ==========================================

    @GetMapping("/my-invitations")
    public ResponseEntity<List<InvitationResponse>> getMyInvitations(
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<InvitationResponse> invitations = teamMemberService.getMyInvitations(userId);
        return ResponseEntity.ok(invitations);
    }

    // ==========================================
    // GET MY ACTIVE TEAMS
    // ==========================================

    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponse>> getMyActiveTeams(
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<TeamResponse> teams = teamMemberService.getMyActiveTeams(userId);
        return ResponseEntity.ok(teams);
    }

    // ==========================================
    // REMOVE MEMBER (Lecturer, Admin, or Team Leader of that team)
    // ==========================================

    @DeleteMapping("/{teamId}/members/{memberId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            Authentication authentication) {

        Long actorId = authUtil.getCurrentUserId(authentication);
        teamMemberService.removeMember(teamId, memberId, actorId);
        return ResponseEntity.noContent().build();
    }
}