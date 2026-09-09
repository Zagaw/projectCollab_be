package com.example.projectCollab.service;

import com.example.projectCollab.dto.ActivityResponse;
import com.example.projectCollab.entity.Activity;
import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public void logActivity(User user, Project project, String action, String description,
                            String entityType, Long entityId) {
        if (user == null || project == null) {
            return;
        }
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setProject(project);
        activity.setAction(action);
        activity.setDescription(description);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);

        activityRepository.save(activity);
    }

    public List<ActivityResponse> getActivitiesForProject(Long projectId, User currentUser) {
        projectAccessService.requireProjectAccess(projectId, currentUser);
        return activityRepository.findActivitiesByProject(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getRecentActivitiesForProject(Long projectId, User currentUser, int limit) {
        projectAccessService.requireProjectAccess(projectId, currentUser);
        return activityRepository.findRecentActivitiesByProject(projectId, limit)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ActivityResponse> getRecentActivitiesForUser(Long userId, User currentUser, int limit) {
        boolean isSelf = currentUser.getUserId().equals(userId);
        boolean isAdminOrLecturer = currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.LECTURER;
        if (!isSelf && !isAdminOrLecturer) {
            throw new UnauthorizedAccessException("You don't have permission to view these activities");
        }
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
