package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ActivityResponse;
import com.example.projectCollab.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/api/projects/{projectId}/activities")
    public ResponseEntity<List<ActivityResponse>> getProjectActivities(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForProject(projectId, limit)
        );
    }

    @GetMapping("/api/users/{userId}/activities")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForUser(userId, limit)
        );
    }

    @GetMapping("/api/projects/{projectId}/activities/all")
    public ResponseEntity<List<ActivityResponse>> getAllProjectActivities(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                activityService.getActivitiesForProject(projectId)
        );
    }

    @GetMapping("/api/users/me/activities")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @RequestParam(defaultValue = "50") int limit) {
        // Will be implemented with current user from Security Context
        // For now, you can use the service method that takes userId
        return ResponseEntity.ok(null);
    }
}