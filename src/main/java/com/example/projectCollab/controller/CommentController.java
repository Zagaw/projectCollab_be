package com.example.projectCollab.controller;

import com.example.projectCollab.dto.CommentRequest;
import com.example.projectCollab.dto.CommentResponse;
import com.example.projectCollab.dto.FileResponse;
import com.example.projectCollab.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ============ TASK COMMENTS ============

    @PostMapping(value = "/api/tasks/{taskId}/comments", 
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> addCommentToTask(
            @PathVariable Long taskId,
            @Valid @ModelAttribute CommentRequest request) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addCommentToTask(taskId, request));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsForTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getCommentsForTask(taskId));
    }

    // ============ PROJECT COMMENTS ============

    @PostMapping(value = "/api/projects/{projectId}/comments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> addCommentToProject(
            @PathVariable Long projectId,
            @Valid @ModelAttribute CommentRequest request) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addCommentToProject(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(commentService.getCommentsForProject(projectId));
    }

    // ============ COMMENT CRUD ============

    @GetMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    @GetMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesForComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getRepliesForComment(commentId));
    }

    @GetMapping("/api/comments/{commentId}/files")
    public ResponseEntity<List<FileResponse>> getFilesForComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getFilesForComment(commentId));
    }

    @PutMapping(value = "/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/comments/{commentId}/hard")
    public ResponseEntity<Void> hardDeleteComment(@PathVariable Long commentId) throws IOException {
        commentService.hardDeleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // ============ FILE OPERATIONS ============

    @DeleteMapping("/api/comments/files/{fileId}")
    public ResponseEntity<Void> deleteFileFromComment(@PathVariable Long fileId) throws IOException {
        commentService.deleteFileFromComment(fileId);
        return ResponseEntity.noContent().build();
    }
}