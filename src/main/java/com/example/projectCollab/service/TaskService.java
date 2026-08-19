package com.example.projectCollab.service;

import com.example.projectCollab.dto.TaskRequest;
import com.example.projectCollab.dto.TaskResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        // Get current user
        User currentUser = getCurrentUser();

        // Validate project exists
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        // Build task
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setProject(project);
        task.setCreatedBy(currentUser);

        // Set status and priority
        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + request.getStatus());
            }
        }
        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + request.getPriority());
            }
        }

        // Set team if provided
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + request.getTeamId()));
            task.setTeam(team);
        }

        // Assign to user if provided
        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }

        // Link to milestone if provided
        if (request.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone not found with id: " + request.getMilestoneId()));
            task.setMilestone(milestone);
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public TaskResponse getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        return mapToResponse(task);
    }

    public List<TaskResponse> getTasksByProject(Long projectId) {
        // Verify project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByTeam(Long teamId) {
        // Verify team exists
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found with id: " + teamId);
        }
        return taskRepository.findByTeamTeamId(teamId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksAssignedToStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("User not found with id: " + studentId);
        }
        return taskRepository.findByAssignedToUserId(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByMilestone(Long milestoneId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new ResourceNotFoundException("Milestone not found with id: " + milestoneId);
        }
        return taskRepository.findByMilestoneMilestoneId(milestoneId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyAssignedTasks() {
        User currentUser = getCurrentUser();
        return taskRepository.findByAssignedToUserId(currentUser.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyOverdueTasks() {
        User currentUser = getCurrentUser();
        return taskRepository.findOverdueTasksForUser(currentUser.getUserId(), LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Check authorization - only creator, assigned user, or admin can update
        User currentUser = getCurrentUser();
        validateTaskAccess(task, currentUser);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getStatus() != null) {
            try {
                TaskStatus newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
                task.setStatus(newStatus);
                if (newStatus == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
                    task.setCompletedAt(LocalDateTime.now());
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + request.getStatus());
            }
        }
        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + request.getPriority());
            }
        }
        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + request.getTeamId()));
            task.setTeam(team);
        }
        if (request.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone not found with id: " + request.getMilestoneId()));
            task.setMilestone(milestone);
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = getCurrentUser();
        validateTaskAccess(task, currentUser);

        try {
            TaskStatus newStatus = TaskStatus.valueOf(status.toUpperCase());
            task.setStatus(newStatus);

            // If completed, set completion time
            if (newStatus == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            } else {
                // If status changed from COMPLETED, clear completion time
                task.setCompletedAt(null);
            }

            Task updatedTask = taskRepository.save(task);
            return mapToResponse(updatedTask);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = getCurrentUser();
        // Only creator or admin can delete
        if (!task.getCreatedBy().getUserId().equals(currentUser.getUserId()) &&
                !currentUser.getRole().equals("ADMIN") &&
                !currentUser.getRole().equals("LECTURER")) {
            throw new UnauthorizedAccessException("You are not authorized to delete this task");
        }

        taskRepository.delete(task);
    }

    // Helper method to get current user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }

        // Assuming your UserDetailsService returns User entity with username field
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Helper method to validate task access
    private void validateTaskAccess(Task task, User user) {
        boolean isCreator = task.getCreatedBy().getUserId().equals(user.getUserId());
        boolean isAssigned = task.getAssignedTo() != null &&
                task.getAssignedTo().getUserId().equals(user.getUserId());
        boolean isAdmin = "ADMIN".equals(user.getRole()) || "LECTURER".equals(user.getRole());

        if (!isCreator && !isAssigned && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to modify this task");
        }
    }

    // Helper method to map Task to TaskResponse
    private TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getTaskId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus().name());
        response.setPriority(task.getPriority().name());
        response.setDeadline(task.getDeadline());
        response.setCompletedAt(task.getCompletedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        // Check if overdue
        if (task.getDeadline() != null &&
                task.getDeadline().isBefore(LocalDateTime.now()) &&
                task.getStatus() != TaskStatus.COMPLETED) {
            response.setOverdue(true);
        }

        // Project info
        if (task.getProject() != null) {
            response.setProjectId(task.getProject().getProjectId());
            response.setProjectTitle(task.getProject().getTitle());
        }

        // Team info
        if (task.getTeam() != null) {
            response.setTeamId(task.getTeam().getTeamId());
            response.setTeamName(task.getTeam().getName());
        }

        // Assigned user info
        if (task.getAssignedTo() != null) {
            response.setAssignedTo(task.getAssignedTo().getUserId());
            response.setAssignedToName(task.getAssignedTo().getName());
        }

        // Creator info
        if (task.getCreatedBy() != null) {
            response.setCreatedBy(task.getCreatedBy().getUserId());
            response.setCreatedByName(task.getCreatedBy().getName());
        }

        // Milestone info
        if (task.getMilestone() != null) {
            response.setMilestoneId(task.getMilestone().getMilestoneId());
            response.setMilestoneTitle(task.getMilestone().getTitle());
        }

        return response;
    }
}