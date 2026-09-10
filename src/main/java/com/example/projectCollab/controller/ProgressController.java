package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ProjectProgressResponse;
import com.example.projectCollab.dto.TeamProgressResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.ProgressService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final AuthUtil authUtil;

    @GetMapping("/api/teams/{teamId}/progress")
    public ResponseEntity<TeamProgressResponse> getTeamProgress(
            @PathVariable Long teamId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(progressService.getTeamProgress(teamId, currentUser));
    }

    @GetMapping("/api/projects/{projectId}/progress")
    public ResponseEntity<ProjectProgressResponse> getProjectProgress(
            @PathVariable Long projectId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(progressService.getProjectProgress(projectId, currentUser));
    }

    @GetMapping("/api/progress/my")
    public ResponseEntity<List<TeamProgressResponse>> getMyProgress(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(progressService.getMyProgress(currentUser));
    }

    @GetMapping("/api/progress/lecturer")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<ProjectProgressResponse>> getLecturerProgress(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(progressService.getLecturerProgress(currentUser));
    }
}
