package com.example.projectCollab.service;

import com.example.projectCollab.dto.DiscussionReplyResponse;
import com.example.projectCollab.dto.DiscussionRequest;
import com.example.projectCollab.dto.DiscussionResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.DiscussionRepository;
import com.example.projectCollab.repository.DiscussionReplyRepository;
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
public class DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final DiscussionReplyRepository discussionReplyRepository;
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
    public DiscussionResponse createDiscussion(Long projectId, DiscussionRequest request) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Discussion discussion = new Discussion();
        discussion.setTitle(request.getTitle());
        discussion.setContent(request.getContent());
        discussion.setProject(project);
        discussion.setCreatedBy(currentUser);

        Discussion savedDiscussion = discussionRepository.save(discussion);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " started a discussion: " + request.getTitle();
        activityService.logActivity(
                currentUser,
                project,
                "DISCUSSION_CREATED",
                description,
                "DISCUSSION",
                savedDiscussion.getDiscussionId()
        );

        return mapToResponse(savedDiscussion);
    }

    @Transactional
    public DiscussionReplyResponse addReplyToDiscussion(Long discussionId, String content) {
        User currentUser = getCurrentUser();

        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found"));

        DiscussionReply reply = new DiscussionReply();
        reply.setContent(content);
        reply.setDiscussion(discussion);
        reply.setUser(currentUser);

        DiscussionReply savedReply = discussionReplyRepository.save(reply);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " replied to discussion: " + discussion.getTitle();
        activityService.logActivity(
                currentUser,
                discussion.getProject(),
                "DISCUSSION_REPLY",
                description,
                "DISCUSSION",
                discussionId
        );

        return mapToReplyResponse(savedReply);
    }

    public List<DiscussionResponse> getDiscussionsForProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        List<Discussion> discussions = discussionRepository.findByProjectProjectIdOrderByCreatedAtDesc(projectId);
        return discussions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DiscussionResponse getDiscussionById(Long discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found"));
        return mapToResponse(discussion);
    }

    @Transactional
    public void deleteDiscussion(Long discussionId) {
        User currentUser = getCurrentUser();
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found"));

        // Only creator or admin can delete
        if (!discussion.getCreatedBy().getUserId().equals(currentUser.getUserId()) &&
                !"ADMIN".equals(currentUser.getRole()) &&
                !"LECTURER".equals(currentUser.getRole())) {
            throw new UnauthorizedAccessException("You are not authorized to delete this discussion");
        }

        discussionRepository.delete(discussion);
    }

    private DiscussionResponse mapToResponse(Discussion discussion) {
        DiscussionResponse response = new DiscussionResponse();
        response.setDiscussionId(discussion.getDiscussionId());
        response.setTitle(discussion.getTitle());
        response.setContent(discussion.getContent());
        response.setCreatedBy(discussion.getCreatedBy().getUserId());
        response.setCreatedByName(discussion.getCreatedBy().getFirstName() + " " +
                discussion.getCreatedBy().getLastName());
        response.setCreatedAt(discussion.getCreatedAt());
        response.setUpdatedAt(discussion.getUpdatedAt());

        List<DiscussionReplyResponse> replyResponses = discussion.getReplies().stream()
                .map(this::mapToReplyResponse)
                .collect(Collectors.toList());
        response.setReplies(replyResponses);

        return response;
    }

    private DiscussionReplyResponse mapToReplyResponse(DiscussionReply reply) {
        DiscussionReplyResponse response = new DiscussionReplyResponse();
        response.setReplyId(reply.getReplyId());
        response.setContent(reply.getContent());
        response.setUserId(reply.getUser().getUserId());
        response.setUserName(reply.getUser().getFirstName() + " " + reply.getUser().getLastName());
        response.setCreatedAt(reply.getCreatedAt());
        return response;
    }
}