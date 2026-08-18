package com.example.projectCollab.controller;

import com.example.projectCollab.dto.MilestoneRequest;
import com.example.projectCollab.dto.MilestoneResponse;
import com.example.projectCollab.dto.MilestoneStatusUpdateRequest;
import com.example.projectCollab.service.MilestoneService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final AuthUtil authUtil;

    public MilestoneController(MilestoneService milestoneService, AuthUtil authUtil) {
        this.milestoneService = milestoneService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // CREATE MILESTONE (Team Leader only)
    // ==========================================
    @PostMapping
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<MilestoneResponse> createMilestone(
            @Valid @RequestBody MilestoneRequest request,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        MilestoneResponse response = milestoneService.createMilestone(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // GET MILESTONE BY ID
    // ==========================================
    @GetMapping("/{milestoneId}")
    public ResponseEntity<MilestoneResponse> getMilestoneById(
            @PathVariable Long milestoneId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        MilestoneResponse response = milestoneService.getMilestoneById(milestoneId, userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET MILESTONES BY TEAM
    // ==========================================
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<MilestoneResponse>> getMilestonesByTeam(
            @PathVariable Long teamId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<MilestoneResponse> milestones = milestoneService.getMilestonesByTeam(teamId, userId);
        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // GET MY TEAM MILESTONES (Team Leader only)
    // ==========================================
    @GetMapping("/my-team-milestones")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<List<MilestoneResponse>> getMyTeamMilestones(
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<MilestoneResponse> milestones = milestoneService.getMyTeamMilestones(userId);
        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // GET INCOMPLETE MILESTONES
    // ==========================================
    @GetMapping("/team/{teamId}/incomplete")
    public ResponseEntity<List<MilestoneResponse>> getIncompleteMilestones(
            @PathVariable Long teamId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<MilestoneResponse> milestones = milestoneService.getIncompleteMilestones(teamId, userId);
        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // GET OVERDUE MILESTONES
    // ==========================================
    @GetMapping("/team/{teamId}/overdue")
    public ResponseEntity<List<MilestoneResponse>> getOverdueMilestones(
            @PathVariable Long teamId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        List<MilestoneResponse> milestones = milestoneService.getOverdueMilestones(teamId, userId);
        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // UPDATE MILESTONE (Team Leader only)
    // ==========================================
    @PutMapping("/{milestoneId}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<MilestoneResponse> updateMilestone(
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneRequest request,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        MilestoneResponse response = milestoneService.updateMilestone(milestoneId, request, userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // UPDATE MILESTONE STATUS (Team Leader only)
    // ==========================================
    @PatchMapping("/{milestoneId}/status")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<MilestoneResponse> updateMilestoneStatus(
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneStatusUpdateRequest request,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        MilestoneResponse response = milestoneService.updateMilestoneStatus(
                milestoneId, request.getIsCompleted(), userId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // DELETE MILESTONE (Team Leader only)
    // ==========================================
    @DeleteMapping("/{milestoneId}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<Void> deleteMilestone(
            @PathVariable Long milestoneId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        milestoneService.deleteMilestone(milestoneId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // GET MILESTONE STATISTICS (Team Leader only)
    // ==========================================
    @GetMapping("/team/{teamId}/statistics")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')")
    public ResponseEntity<MilestoneService.MilestoneStatistics> getMilestoneStatistics(
            @PathVariable Long teamId,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        MilestoneService.MilestoneStatistics statistics = 
                milestoneService.getMilestoneStatistics(teamId, userId);
        return ResponseEntity.ok(statistics);
    }
}