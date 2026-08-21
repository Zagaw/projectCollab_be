package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ActivityResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.repository.UserRepository;
import com.example.projectCollab.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final UserRepository userRepository;

    /**
     * Get recent activities for a project
     * GET /api/projects/{projectId}/activities?limit=50
     */
    @GetMapping("/api/projects/{projectId}/activities")
    public ResponseEntity<List<ActivityResponse>> getProjectActivities(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForProject(projectId, limit)
        );
    }

    /**
     * Get all activities for a project
     * GET /api/projects/{projectId}/activities/all
     */
    @GetMapping("/api/projects/{projectId}/activities/all")
    public ResponseEntity<List<ActivityResponse>> getAllProjectActivities(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                activityService.getActivitiesForProject(projectId)
        );
    }

    /**
     * Get activities for a specific user
     * GET /api/users/{userId}/activities?limit=50
     */
    @GetMapping("/api/users/{userId}/activities")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForUser(userId, limit)
        );
    }

    /**
     * Get activities for the current user
     * GET /api/users/me/activities?limit=50
     */
    @GetMapping("/api/users/me/activities")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @RequestParam(defaultValue = "50") int limit) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForUser(currentUser.getUserId(), limit)
        );
    }

    /**
     * Get current user from Security Context
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}