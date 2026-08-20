package com.example.projectCollab.service;

import com.example.projectCollab.dto.TaskRequest;
import com.example.projectCollab.dto.TaskResponse;
import com.example.projectCollab.dto.TaskSummaryResponse;
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
    private final TeamMemberRepository teamMemberRepository;  // Add this

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setProject(project);
        task.setCreatedBy(currentUser);

        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + request.getStatus() +
                        ". Allowed values: TODO, IN_PROGRESS, COMPLETED, BLOCKED, REVIEW");
            }
        }
        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + request.getPriority() +
                        ". Allowed values: LOW, MEDIUM, HIGH, URGENT");
            }
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
            task.setTeam(team);
        }

        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }

        if (request.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", request.getMilestoneId()));
            task.setMilestone(milestone);
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public TaskResponse getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // ✅ FIX: Check if user has access to this task
        User currentUser = getCurrentUser();
        if (!hasAccessToTask(task, currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view this task");
        }

        return mapToResponse(task);
    }

    // ✅ FIX: Add authorization check
    public List<TaskResponse> getTasksByProject(Long projectId) {
        User currentUser = getCurrentUser();

        // Check if user has access to this project
        if (!hasAccessToProject(projectId, currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view tasks for this project");
        }

        return taskRepository.findByProjectProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ✅ FIX: Add authorization check
    public List<TaskResponse> getTasksByTeam(Long teamId) {
        User currentUser = getCurrentUser();

        // Check if user has access to this team
        if (!hasAccessToTeam(teamId, currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view tasks for this team");
        }

        return taskRepository.findByTeamTeamId(teamId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksAssignedToStudent(Long studentId) {
        // ✅ FIX: Only lecturers and admins can view other students' tasks
        User currentUser = getCurrentUser();
        boolean isAdminOrLecturer = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.LECTURER;
        boolean isSelf = currentUser.getUserId().equals(studentId);

        if (!isAdminOrLecturer && !isSelf) {
            throw new UnauthorizedAccessException("You don't have permission to view other students' tasks");
        }

        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("User", "id", studentId);
        }
        return taskRepository.findByAssignedToUserId(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ✅ FIX: Add authorization check
    public List<TaskResponse> getTasksByMilestone(Long milestoneId) {
        User currentUser = getCurrentUser();

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));

        // Check if user has access to the milestone's team
        if (!hasAccessToTeam(milestone.getTeam().getTeamId(), currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view tasks for this milestone");
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

    // ✅ FIX: Add authorization check
    public TaskSummaryResponse getTaskSummaryByProject(Long projectId) {
        User currentUser = getCurrentUser();

        // Check if user has access to this project
        if (!hasAccessToProject(projectId, currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to view task summary for this project");
        }

        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        List<Task> tasks = taskRepository.findByProjectProjectId(projectId);
        long total = tasks.size();

        if (total == 0) {
            return new TaskSummaryResponse(0, 0, 0, 0, 0.0);
        }

        long todo = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();
        long inProgress = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();
        long completed = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        double percentage = ((double) completed / total) * 100.0;

        return new TaskSummaryResponse(
                total,
                todo,
                inProgress,
                completed,
                Math.round(percentage * 100.0) / 100.0
        );
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        validateTaskModificationAccess(task, currentUser);

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
                throw new IllegalArgumentException("Invalid status: " + request.getStatus() +
                        ". Allowed values: TODO, IN_PROGRESS, COMPLETED, BLOCKED, REVIEW");
            }
        }
        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + request.getPriority() +
                        ". Allowed values: LOW, MEDIUM, HIGH, URGENT");
            }
        }
        if (request.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }
        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
            task.setTeam(team);
        }
        if (request.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", request.getMilestoneId()));
            task.setMilestone(milestone);
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, String status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();
        validateTaskStatusAccess(task, currentUser);

        try {
            TaskStatus newStatus = TaskStatus.valueOf(status.toUpperCase());
            task.setStatus(newStatus);

            if (newStatus == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setCompletedAt(null);
            }

            Task updatedTask = taskRepository.save(task);
            return mapToResponse(updatedTask);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status +
                    ". Allowed values: TODO, IN_PROGRESS, COMPLETED, BLOCKED, REVIEW");
        }
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User currentUser = getCurrentUser();

        boolean isCreator = task.getCreatedBy().getUserId().equals(currentUser.getUserId());
        boolean isAdminOrLecturer = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.LECTURER;

        if (!isCreator && !isAdminOrLecturer) {
            throw new UnauthorizedAccessException("You are not authorized to delete this task");
        }

        taskRepository.delete(task);
    }

    // ==========================================
    // ✅ NEW: AUTHORIZATION HELPER METHODS
    // ==========================================

    /**
     * Check if user has access to view a task
     * - User is the assigned person
     * - User is the creator
     * - User is a team member of the task's team
     * - User is the lecturer of the project
     * - User is admin
     */
    private boolean hasAccessToTask(Task task, User user) {
        // Admin has full access
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // User is assigned to the task
        if (task.getAssignedTo() != null &&
                task.getAssignedTo().getUserId().equals(user.getUserId())) {
            return true;
        }

        // User created the task
        if (task.getCreatedBy().getUserId().equals(user.getUserId())) {
            return true;
        }

        // Check if user is a team member
        if (task.getTeam() != null) {
            return isTeamMember(task.getTeam().getTeamId(), user.getUserId());
        }

        // Check if user is the lecturer of the project
        if (task.getProject() != null && task.getProject().getLecturer() != null) {
            return task.getProject().getLecturer().getUserId().equals(user.getUserId());
        }

        return false;
    }

    /**
     * Check if user has access to a project
     * - User is the lecturer of the project
     * - User is a member of any team in the project
     * - User is admin
     */
    private boolean hasAccessToProject(Long projectId, User user) {
        // Admin has full access
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // Get the project
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        // Check if user is the lecturer
        if (project.getLecturer() != null &&
                project.getLecturer().getUserId().equals(user.getUserId())) {
            return true;
        }

        // Check if user is a member of any team in this project
        List<Team> teams = teamRepository.findByProject_ProjectId(projectId);
        for (Team team : teams) {
            if (isTeamMember(team.getTeamId(), user.getUserId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user has access to a team
     * - User is a member of the team
     * - User is the lecturer of the project
     * - User is admin
     */
    private boolean hasAccessToTeam(Long teamId, User user) {
        // Admin has full access
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // Check if user is a team member
        if (isTeamMember(teamId, user.getUserId())) {
            return true;
        }

        // Check if user is the lecturer of the project
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team != null && team.getProject() != null && team.getProject().getLecturer() != null) {
            return team.getProject().getLecturer().getUserId().equals(user.getUserId());
        }

        return false;
    }

    /**
     * Check if a user is a member of a team
     */
    private boolean isTeamMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeam_TeamIdAndUser_UserId(teamId, userId).isPresent();
    }

    /**
     * Validate access for task modification (update full task)
     * - User is the creator OR
     * - User is the lecturer OR
     * - User is admin
     */
    private void validateTaskModificationAccess(Task task, User user) {
        boolean isCreator = task.getCreatedBy().getUserId().equals(user.getUserId());
        boolean isAdminOrLecturer = user.getRole() == Role.ADMIN || user.getRole() == Role.LECTURER;

        if (!isCreator && !isAdminOrLecturer) {
            throw new UnauthorizedAccessException("You are not authorized to modify this task");
        }
    }

    /**
     * Validate access for task status update
     * - User is the assigned person OR
     * - User is the creator OR
     * - User is the lecturer OR
     * - User is admin
     */
    private void validateTaskStatusAccess(Task task, User user) {
        boolean isAssigned = task.getAssignedTo() != null &&
                task.getAssignedTo().getUserId().equals(user.getUserId());
        boolean isCreator = task.getCreatedBy().getUserId().equals(user.getUserId());
        boolean isAdminOrLecturer = user.getRole() == Role.ADMIN || user.getRole() == Role.LECTURER;

        if (!isAssigned && !isCreator && !isAdminOrLecturer) {
            throw new UnauthorizedAccessException("You are not authorized to update the status of this task");
        }
    }

    // ==========================================
    // HELPER METHODS (Existing)
    // ==========================================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

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

        if (task.getDeadline() != null &&
                task.getDeadline().isBefore(LocalDateTime.now()) &&
                task.getStatus() != TaskStatus.COMPLETED) {
            response.setOverdue(true);
        }

        if (task.getProject() != null) {
            response.setProjectId(task.getProject().getProjectId());
            response.setProjectTitle(task.getProject().getTitle());
        }

        if (task.getTeam() != null) {
            response.setTeamId(task.getTeam().getTeamId());
            response.setTeamName(task.getTeam().getName());
        }

        if (task.getAssignedTo() != null) {
            response.setAssignedTo(task.getAssignedTo().getUserId());
            response.setAssignedToName(task.getAssignedTo().getUsername());
        }

        if (task.getCreatedBy() != null) {
            response.setCreatedBy(task.getCreatedBy().getUserId());
            response.setCreatedByName(task.getCreatedBy().getUsername());
        }

        if (task.getMilestone() != null) {
            response.setMilestoneId(task.getMilestone().getMilestoneId());
            response.setMilestoneTitle(task.getMilestone().getTitle());
        }

        return response;
    }
}