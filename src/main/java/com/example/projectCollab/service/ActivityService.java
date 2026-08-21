package com.example.projectCollab.service;

import com.example.projectCollab.dto.ActivityResponse;
import com.example.projectCollab.entity.Activity;
import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    @Transactional
    public void logActivity(User user, Project project, String action, String description,
                            String entityType, Long entityId) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setProject(project);
        activity.setAction(action);
        activity.setDescription(description);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);

        activityRepository.save(activity);
    }

    public List<ActivityResponse> getActivitiesForProject(Long projectId) {
        return activityRepository.findByProjectProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getActivitiesForUser(Long userId) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getRecentActivitiesForProject(Long projectId, int limit) {
        return activityRepository.findRecentActivitiesByProject(projectId, limit)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getRecentActivitiesForUser(Long userId, int limit) {
        return activityRepository.findRecentActivitiesByUser(userId, limit)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setActivityId(activity.getActivityId());
        response.setAction(activity.getAction());
        response.setDescription(activity.getDescription());
        response.setEntityType(activity.getEntityType());
        response.setEntityId(activity.getEntityId());
        response.setUserId(activity.getUser().getUserId());
        response.setUserName(activity.getUser().getFirstName() + " " +
                activity.getUser().getLastName());
        response.setCreatedAt(activity.getCreatedAt());

        if (activity.getProject() != null) {
            response.setProjectId(activity.getProject().getProjectId());
            response.setProjectTitle(activity.getProject().getTitle());
        }

        return response;
    }
}