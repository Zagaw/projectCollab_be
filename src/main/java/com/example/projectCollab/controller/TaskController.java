package com.example.projectCollab.controller;

import com.example.projectCollab.dto.TaskRequest;
import com.example.projectCollab.dto.TaskResponse;
import com.example.projectCollab.dto.TaskSummaryResponse;
import com.example.projectCollab.dto.TaskStatusUpdateRequest;
import com.example.projectCollab.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 1. Import ထည့်သွင်းပါ
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task
     * POST /api/tasks
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')") // 2. Leader & Lecturer တည်း ဆောက်ခွင့်ပြုမည်
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get task by ID
     * GET /api/tasks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Get all tasks for a project
     * GET /api/tasks/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    /**
     * Get all tasks for a team
     * GET /api/tasks/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<TaskResponse>> getTasksByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(taskService.getTasksByTeam(teamId));
    }

    /**
     * Get tasks assigned to current student (from JWT)
     * GET /api/tasks/my-tasks
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyAssignedTasks() {
        return ResponseEntity.ok(taskService.getMyAssignedTasks());
    }

    /**
     * Get tasks assigned to a specific student
     * GET /api/tasks/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<TaskResponse>> getTasksAssignedToStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(taskService.getTasksAssignedToStudent(studentId));
    }

    /**
     * Get all tasks for a milestone
     * GET /api/tasks/milestone/{milestoneId}
     */
    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<List<TaskResponse>> getTasksByMilestone(@PathVariable Long milestoneId) {
        return ResponseEntity.ok(taskService.getTasksByMilestone(milestoneId));
    }

    /**
     * Get my overdue tasks
     * GET /api/tasks/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getMyOverdueTasks() {
        return ResponseEntity.ok(taskService.getMyOverdueTasks());
    }

    /**
     * Get task summary for a project
     * GET /api/tasks/project/{projectId}/summary
     */
    @GetMapping("/project/{projectId}/summary")
    public ResponseEntity<TaskSummaryResponse> getTaskSummaryByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTaskSummaryByProject(projectId));
    }

    /**
     * Update a task (full update)
     * PUT /api/tasks/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')") // 3. Leader & Lecturer သာ ပြင်ခွင့်ပြုမည်
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    /**
     * Update task status only
     * PATCH /api/tasks/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEAM_LEADER', 'LECTURER', 'ADMIN')") // 4. Student များပါ Status ပြောင်းခွင့်ပြုမည်
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request.getStatus()));
    }

    /**
     * Delete a task
     * DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'ADMIN')") // 5. Leader & Lecturer သာ ဖျက်ခွင့်ပြုမည်
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}