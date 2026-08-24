package com.example.projectCollab.controller;

import com.example.projectCollab.dto.DiscussionReplyResponse;
import com.example.projectCollab.dto.DiscussionRequest;
import com.example.projectCollab.dto.DiscussionResponse;
import com.example.projectCollab.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    public ResponseEntity<DiscussionResponse> createDiscussion(
            @PathVariable Long projectId,
            @Valid @RequestBody DiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.createDiscussion(projectId, request));
    }

    @GetMapping
    public ResponseEntity<List<DiscussionResponse>> getDiscussions(@PathVariable Long projectId) {
        return ResponseEntity.ok(discussionService.getDiscussionsForProject(projectId));
    }

    @GetMapping("/{discussionId}")
    public ResponseEntity<DiscussionResponse> getDiscussionById(@PathVariable Long discussionId) {
        return ResponseEntity.ok(discussionService.getDiscussionById(discussionId));
    }

    @PostMapping("/{discussionId}/replies")
    public ResponseEntity<DiscussionReplyResponse> addReply(
            @PathVariable Long discussionId,
            @RequestBody String content) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.addReplyToDiscussion(discussionId, content));
    }

    @DeleteMapping("/{discussionId}")
    public ResponseEntity<Void> deleteDiscussion(@PathVariable Long discussionId) {
        discussionService.deleteDiscussion(discussionId);
        return ResponseEntity.noContent().build();
    }
}