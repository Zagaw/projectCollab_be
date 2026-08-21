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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    @Transactional
    public CommentResponse addCommentToProject(Long projectId, CommentRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setProject(project);
        comment.setUser(currentUser);

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

    public List<CommentResponse> getCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepository.findByTaskTaskIdOrderByCreatedAtAsc(taskId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CommentResponse> getCommentsForProject(Long projectId) {
        List<Comment> comments = commentRepository.findByProjectProjectIdOrderByCreatedAtAsc(projectId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Only comment owner or admin can delete
        if (!comment.getUser().getUserId().equals(currentUser.getUserId()) &&
                !"ADMIN".equals(currentUser.getRole()) &&
                !"LECTURER".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment");
        }

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

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getCommentId());
            response.setParentCommentContent(comment.getParentComment().getContent());
        }

        return response;
    }
}