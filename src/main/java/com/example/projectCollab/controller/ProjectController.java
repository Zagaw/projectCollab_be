package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ProjectRequest;
import com.example.projectCollab.dto.ProjectResponse;
import com.example.projectCollab.entity.ProjectStatus;
import com.example.projectCollab.service.ProjectService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuthUtil authUtil;

    public ProjectController(ProjectService projectService, AuthUtil authUtil) {
        this.projectService = projectService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // CREATE PROJECT (Lecturer only)
    // ==========================================

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {

        // FIX: Get actual lecturer ID from authentication
        Long lecturerId = authUtil.getCurrentUserId(authentication);
        System.out.println("📝 Creating project for lecturer ID: " + lecturerId);

        ProjectResponse response = projectService.createProject(request, lecturerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // GET PROJECT BY ID
    // ==========================================

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        ProjectResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET PROJECTS BY LECTURER
    // ==========================================

    @GetMapping("/my-projects")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(Authentication authentication) {
        Long lecturerId = authUtil.getCurrentUserId(authentication);
        List<ProjectResponse> projects = projectService.getProjectsByLecturer(lecturerId);
        return ResponseEntity.ok(projects);
    }

    // ==========================================
    // GET ALL PROJECTS (Admin only)
    // ==========================================

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    // ==========================================
    // UPDATE PROJECT
    // ==========================================

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        ProjectResponse response = projectService.updateProject(projectId, request, lecturerId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // DELETE PROJECT
    // ==========================================

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        projectService.deleteProject(projectId, lecturerId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // UPDATE PROJECT STATUS
    // ==========================================

    @PatchMapping("/{projectId}/status")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ProjectResponse> updateProjectStatus(
            @PathVariable Long projectId,
            @RequestBody ProjectStatus status,
            Authentication authentication) {

        Long lecturerId = authUtil.getCurrentUserId(authentication);
        ProjectResponse response = projectService.updateProjectStatus(projectId, status, lecturerId);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET OVERDUE PROJECTS
    // ==========================================

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<ProjectResponse>> getOverdueProjects() {
        List<ProjectResponse> projects = projectService.getOverdueProjects();
        return ResponseEntity.ok(projects);
    }
}