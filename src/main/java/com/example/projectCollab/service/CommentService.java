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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

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
        comment.setDeleted(false);  // ✅ FIXED - Using setDeleted()

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

    @Transactional
    public CommentResponse addCommentToProject(Long projectId, CommentRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setProject(project);
        comment.setUser(currentUser);
        comment.setDeleted(false);  // ✅ FIXED - Using setDeleted()

        Comment savedComment = commentRepository.save(comment);

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

    public List<CommentResponse> getCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepository.findActiveCommentsByTask(taskId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CommentResponse> getCommentsForProject(Long projectId) {
        List<Comment> comments = commentRepository.findActiveCommentsByProject(projectId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("You are not authorized to edit this comment");
        }

        // ✅ FIXED - Using isDeleted()
        if (comment.isDeleted()) {
            throw new RuntimeException("Cannot edit a deleted comment");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return mapToResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Check authorization
        boolean isOwner = comment.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
        boolean isLecturer = "LECTURER".equals(currentUser.getRole().name());

        if (!isOwner && !isAdmin && !isLecturer) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment");
        }

        // ✅ FIXED - Using setDeleted()
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    @Transactional
    public void hardDeleteComment(Long commentId) {
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole().name())) {
            throw new UnauthorizedAccessException("Only admin can permanently delete comments");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        commentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getUserId());
        response.setUserName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setDeleted(comment.isDeleted());  // ✅ FIXED - Using isDeleted()

        // ✅ FIXED - Using getReplies()
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            long replyCount = comment.getReplies().stream()
                    .filter(reply -> !reply.isDeleted())
                    .count();
            response.setReplyCount((int) replyCount);
        } else {
            response.setReplyCount(0);
        }

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getCommentId());
            response.setParentCommentContent(comment.getParentComment().getContent());
        }

        return response;
    }
}