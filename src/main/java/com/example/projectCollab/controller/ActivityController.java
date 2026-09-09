package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ActivityResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.ActivityService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final AuthUtil authUtil;

    @GetMapping("/api/projects/{projectId}/activities")
    public ResponseEntity<List<ActivityResponse>> getProjectActivities(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForProject(projectId, currentUser, limit)
        );
    }

    @GetMapping("/api/projects/{projectId}/activities/all")
    public ResponseEntity<List<ActivityResponse>> getAllProjectActivities(
            @PathVariable Long projectId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                activityService.getActivitiesForProject(projectId, currentUser)
        );
    }

    @GetMapping("/api/users/{userId}/activities")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForUser(userId, currentUser, limit)
        );
    }

    @GetMapping("/api/users/me/activities")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(
                activityService.getRecentActivitiesForUser(currentUser.getUserId(), currentUser, limit)
        );
    }
}
