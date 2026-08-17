package com.example.projectCollab.controller;

import com.example.projectCollab.dto.TeamRequest;
import com.example.projectCollab.dto.TeamResponse;
import com.example.projectCollab.service.TeamService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final AuthUtil authUtil;

    public TeamController(TeamService teamService, AuthUtil authUtil) {
        this.teamService = teamService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // CREATE TEAM (Lecturer only)
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody TeamRequest request,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        TeamResponse response = teamService.createTeam(request, lecturerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // GET TEAM BY ID
    // ==========================================

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        TeamResponse response = teamService.getTeamById(teamId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET TEAMS BY PROJECT
    // ==========================================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByProject(@PathVariable Long projectId) {
        List<TeamResponse> teams = teamService.getTeamsByProject(projectId);
        return ResponseEntity.ok(teams);
    }

    // ==========================================
    // GET MY TEAMS
    // ==========================================

    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponse>> getMyTeams(Authentication authentication) {
        Long userId = authUtil.getCurrentUserId(authentication);
        List<TeamResponse> teams = teamService.getTeamsByMember(userId);
        return ResponseEntity.ok(teams);
    }

    // ==========================================
    // UPDATE TEAM
    // ==========================================

    @PutMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRequest request,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        TeamResponse response = teamService.updateTeam(teamId, request, lecturerId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // DELETE TEAM
    // ==========================================

    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        teamService.deleteTeam(teamId, lecturerId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // ASSIGN TEAM LEADER
    // ==========================================

    @PostMapping("/{teamId}/assign-leader/{userId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<TeamResponse> assignTeamLeader(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        TeamResponse response = teamService.assignTeamLeader(teamId, userId, lecturerId);
        return ResponseEntity.ok(response);
    }
}