package com.example.projectCollab.service;

import com.example.projectCollab.dto.CommentRequest;
import com.example.projectCollab.dto.CommentResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.CommentRepository;
import com.example.projectCollab.repository.TaskRepository;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.UserRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

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

    /**
     * Add comment to a task
     */
    @Transactional
    public CommentResponse addCommentToTask(Long taskId, CommentRequest request) {
        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setTask(task);
        comment.setUser(currentUser);
        comment.setProject(task.getProject());
        comment.setDeleted(false);

        // Handle reply to comment
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " commented on task: " + task.getTitle();
        activityService.logActivity(
                currentUser,
                task.getProject(),
                "COMMENT_ADDED",
                description,
                "TASK",
                taskId
        );

        return mapToResponse(savedComment);
    }

    /**
     * Add comment to a project
     */
    @Transactional
    public CommentResponse addCommentToProject(Long projectId, CommentRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setProject(project);
        comment.setUser(currentUser);
        comment.setDeleted(false);

        Comment savedComment = commentRepository.save(comment);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " commented on project: " + project.getTitle();
        activityService.logActivity(
                currentUser,
                project,
                "PROJECT_COMMENT_ADDED",
                description,
                "PROJECT",
                projectId
        );

        return mapToResponse(savedComment);
    }

    /**
     * Get all active comments for a task
     */
    public List<CommentResponse> getCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepository.findActiveCommentsByTask(taskId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active comments for a project
     */
    public List<CommentResponse> getCommentsForProject(Long projectId) {
        List<Comment> comments = commentRepository.findActiveCommentsByProject(projectId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a comment (edit content)
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Only comment owner can edit
        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("You are not authorized to edit this comment");
        }

        if (comment.isDeleted()) {
            throw new RuntimeException("Cannot edit a deleted comment");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return mapToResponse(updatedComment);
    }

    /**
     * Soft delete a comment
     */
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Only comment owner or admin/lecturer can delete
        if (!comment.getUser().getUserId().equals(currentUser.getUserId()) &&
                !"ADMIN".equals(currentUser.getRole()) &&
                !"LECTURER".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment");
        }

        // Soft delete (just mark as deleted)
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    /**
     * Hard delete a comment (admin only)
     */
    @Transactional
    public void hardDeleteComment(Long commentId) {
        User currentUser = getCurrentUser();

        // Only admin can hard delete
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("Only admin can permanently delete comments");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        commentRepository.delete(comment);
    }

    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getUserId());
        response.setUserName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setDeleted(comment.isDeleted());

        // Set reply count
        if (comment.getReplies() != null) {
            response.setReplyCount((int) comment.getReplies().stream()
                    .filter(reply -> !reply.isDeleted())
                    .count());
        }

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getCommentId());
            response.setParentCommentContent(comment.getParentComment().getContent());
        }

        return response;
    }
}