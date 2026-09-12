package com.example.projectCollab.service;

import com.example.projectCollab.dto.NotificationResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_LIMIT = 30;

    private final NotificationRepository notificationRepository;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public boolean notifyIfAbsent(User recipient, String type, String title, String message,
                                  String entityType, Long entityId, Long projectId, LocalDateTime since) {
        if (recipient == null || entityId == null) {
            return false;
        }
        boolean exists = notificationRepository.existsByRecipient_UserIdAndTypeAndEntityIdAndCreatedAtAfter(
                recipient.getUserId(), type, entityId, since);
        if (exists) {
            return false;
        }
        notifyUsers(null, List.of(recipient), type, title, fitMessage(message), entityType, entityId, projectId);
        return true;
    }

    @Transactional
    public void notifyUsers(User actor, List<User> recipients, String type, String title, String message,
                            String entityType, Long entityId, Long projectId) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        List<Notification> toSave = new ArrayList<>();
        for (User recipient : recipients) {
            if (recipient == null) {
                continue;
            }
            if (actor != null && actor.getUserId().equals(recipient.getUserId())) {
                continue;
            }
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setActor(actor);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setEntityType(entityType);
            notification.setEntityId(entityId);
            notification.setProjectId(projectId);
            notification.setRead(false);
            toSave.add(notification);
        }
        if (!toSave.isEmpty()) {
            notificationRepository.saveAll(toSave);
        }
    }

    @Transactional
    public void notifyLecturerVerified(User lecturer) {
        if (lecturer == null) {
            return;
        }
        notifyUsers(
                null,
                List.of(lecturer),
                "LECTURER_VERIFIED",
                "Account approved",
                "An administrator approved your lecturer account. You can now use Collabora.",
                "USER",
                lecturer.getUserId(),
                null
        );
    }

    @Transactional
    public void notifyTaskAssigned(User actor, Task task) {
        if (task.getAssignedTo() == null) {
            return;
        }
        Long projectId = task.getProject() != null ? task.getProject().getProjectId() : null;
        notifyUsers(
                actor,
                List.of(task.getAssignedTo()),
                "TASK_ASSIGNED",
                "Task assigned to you",
                displayName(actor) + " assigned you the task \"" + task.getTitle() + "\"",
                "TASK",
                task.getTaskId(),
                projectId
        );
    }

    @Transactional
    public void notifyMemberInvited(User actor, User student, Team team) {
        Long projectId = team.getProject() != null ? team.getProject().getProjectId() : null;
        notifyUsers(
                actor,
                List.of(student),
                "MEMBER_INVITED",
                "Team invitation",
                displayName(actor) + " invited you to join " + team.getName(),
                "TEAM",
                team.getTeamId(),
                projectId
        );
    }

    @Transactional
    public void notifyCommentOnTask(User actor, Task task, String preview) {
        List<User> recipients = new ArrayList<>();
        if (task.getAssignedTo() != null) {
            recipients.add(task.getAssignedTo());
        }
        if (task.getCreatedBy() != null) {
            recipients.add(task.getCreatedBy());
        }
        Long projectId = task.getProject() != null ? task.getProject().getProjectId() : null;
        notifyUsers(
                actor,
                recipients,
                "TASK_COMMENT",
                "New comment on a task",
                displayName(actor) + " commented on \"" + task.getTitle() + "\": " + preview,
                "TASK",
                task.getTaskId(),
                projectId
        );
    }

    @Transactional
    public void notifyCommentOnProject(User actor, Project project, String preview) {
        notifyUsers(
                actor,
                projectAccessService.getProjectNotifyRecipients(project),
                "PROJECT_COMMENT",
                "New project comment",
                displayName(actor) + " commented on " + project.getTitle() + ": " + preview,
                "COMMENT",
                project.getProjectId(),
                project.getProjectId()
        );
    }

    @Transactional
    public void notifyDiscussionEvent(User actor, Project project, Discussion discussion, boolean isReply) {
        notifyUsers(
                actor,
                projectAccessService.getProjectNotifyRecipients(project),
                isReply ? "DISCUSSION_REPLY" : "DISCUSSION_CREATED",
                isReply ? "New discussion reply" : "New discussion",
                displayName(actor) + (isReply ? " replied in \"" : " started \"") + discussion.getTitle() + "\"",
                "DISCUSSION",
                discussion.getDiscussionId(),
                project.getProjectId()
        );
    }

    @Transactional
    public void notifyFileUploaded(User actor, Project project, File file) {
        notifyUsers(
                actor,
                projectAccessService.getProjectNotifyRecipients(project),
                "FILE_UPLOADED",
                "New file uploaded",
                displayName(actor) + " uploaded " + file.getFileName(),
                "FILE",
                file.getFileId(),
                project.getProjectId()
        );
    }

    @Transactional
    public void notifyMilestoneCreated(User actor, Team team, Milestone milestone) {
        Long projectId = team.getProject() != null ? team.getProject().getProjectId() : null;
        notifyUsers(
                actor,
                projectAccessService.getActiveTeamMembers(team.getTeamId()),
                "MILESTONE_CREATED",
                "New milestone",
                displayName(actor) + " created milestone \"" + milestone.getTitle() + "\"",
                "MILESTONE",
                milestone.getMilestoneId(),
                projectId
        );
    }

    @Transactional
    public void notifyMeetingScheduled(User actor, Team team, Meeting meeting) {
        notifyMeetingEvent(actor, team, meeting, "MEETING_SCHEDULED", "Meeting scheduled",
                displayName(actor) + " scheduled \"" + meeting.getTitle() + "\"");
    }

    @Transactional
    public void notifyMeetingUpdated(User actor, Team team, Meeting meeting) {
        notifyMeetingEvent(actor, team, meeting, "MEETING_UPDATED", "Meeting updated",
                displayName(actor) + " updated \"" + meeting.getTitle() + "\"");
    }

    @Transactional
    public void notifyMeetingCancelled(User actor, Team team, Meeting meeting) {
        notifyMeetingEvent(actor, team, meeting, "MEETING_CANCELLED", "Meeting cancelled",
                displayName(actor) + " cancelled \"" + meeting.getTitle() + "\"");
    }

    @Transactional
    public void notifyMeetingMinutes(User actor, Team team, Meeting meeting) {
        notifyMeetingEvent(actor, team, meeting, "MEETING_MINUTES", "Meeting minutes posted",
                displayName(actor) + " posted minutes for \"" + meeting.getTitle() + "\"");
    }

    private void notifyMeetingEvent(User actor, Team team, Meeting meeting,
                                    String type, String title, String message) {
        Long projectId = team.getProject() != null ? team.getProject().getProjectId() : null;
        notifyUsers(
                actor,
                projectAccessService.getTeamNotifyRecipients(team),
                type,
                title,
                message,
                "MEETING",
                meeting.getMeetingId(),
                projectId
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(User currentUser, boolean unreadOnly) {
        PageRequest page = PageRequest.of(0, DEFAULT_LIMIT);
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findUnreadByRecipientUserId(currentUser.getUserId(), page)
                : notificationRepository.findByRecipientUserId(currentUser.getUserId(), page);
        return notifications.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countUnreadByRecipientUserId(currentUser.getUserId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository
                .findByNotificationIdAndRecipientUserId(notificationId, currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getRecipient() == null
                || !notification.getRecipient().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("You don't have permission to update this notification");
        }
        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(User currentUser) {
        notificationRepository.markAllReadForUser(currentUser.getUserId());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setEntityType(notification.getEntityType());
        response.setEntityId(notification.getEntityId());
        response.setProjectId(notification.getProjectId());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        if (notification.getActor() != null) {
            response.setActorId(notification.getActor().getUserId());
            response.setActorName(displayName(notification.getActor()));
        }
        return response;
    }

    private String displayName(User user) {
        if (user == null) {
            return "Someone";
        }
        return user.getFirstName() + " " + user.getLastName();
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed;
    }

    public String clip(String content) {
        return preview(content);
    }

    private String fitMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 500 ? message.substring(0, 497) + "..." : message;
    }
}
