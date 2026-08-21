package com.example.projectCollab.controller;

import com.example.projectCollab.dto.CommentRequest;
import com.example.projectCollab.dto.CommentResponse;
import com.example.projectCollab.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Task Comments
    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> addCommentToTask(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addCommentToTask(taskId, request));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsForTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getCommentsForTask(taskId));
    }

    // Project Comments
    @PostMapping("/api/projects/{projectId}/comments")
    public ResponseEntity<CommentResponse> addCommentToProject(
            @PathVariable Long projectId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addCommentToProject(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(commentService.getCommentsForProject(projectId));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}