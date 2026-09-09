package com.example.projectCollab.controller;

import com.example.projectCollab.dto.FileResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.ProjectFileService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectFileController {

    private final ProjectFileService projectFileService;
    private final AuthUtil authUtil;

    @PostMapping("/api/projects/{projectId}/files")
    public ResponseEntity<FileResponse> uploadProjectFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "teamId", required = false) Long teamId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                projectFileService.uploadToProject(projectId, file, category, teamId, currentUser)
        );
    }

    @GetMapping("/api/projects/{projectId}/files")
    public ResponseEntity<List<FileResponse>> listProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "teamId", required = false) Long teamId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                projectFileService.listProjectFiles(projectId, category, teamId, currentUser)
        );
    }

    @PostMapping("/api/teams/{teamId}/files")
    public ResponseEntity<FileResponse> uploadTeamFile(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                projectFileService.uploadToTeam(teamId, file, category, currentUser)
        );
    }

    @GetMapping("/api/teams/{teamId}/files")
    public ResponseEntity<List<FileResponse>> listTeamFiles(
            @PathVariable Long teamId,
            @RequestParam(value = "category", required = false) String category,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                projectFileService.listTeamFiles(teamId, category, currentUser)
        );
    }
}
