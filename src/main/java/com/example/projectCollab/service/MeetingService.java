package com.example.projectCollab.service;

import com.example.projectCollab.dto.MeetingAttendeeResponse;
import com.example.projectCollab.dto.MeetingRequest;
import com.example.projectCollab.dto.MeetingResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.MeetingAttendeeRepository;
import com.example.projectCollab.repository.MeetingRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    @Transactional
    public MeetingResponse createMeeting(Long teamId, MeetingRequest request, User currentUser) {
        Team team = projectAccessService.requireTeam(teamId);
        requireCanManage(team, currentUser);
        validateTimes(request.getStartAt(), request.getEndAt());

        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle().trim());
        meeting.setAgenda(blankToNull(request.getAgenda()));
        meeting.setStartAt(request.getStartAt());
        meeting.setEndAt(request.getEndAt());
        meeting.setLocation(blankToNull(request.getLocation()));
        meeting.setMeetingLink(blankToNull(request.getMeetingLink()));
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setTeam(team);
        meeting.setCreatedBy(currentUser);

        Meeting saved = meetingRepository.save(meeting);
        activityService.logActivity(
                currentUser,
                team.getProject(),
                "MEETING_CREATED",
                displayName(currentUser) + " scheduled meeting \"" + saved.getTitle() + "\"",
                "MEETING",
                saved.getMeetingId()
        );
        notificationService.notifyMeetingScheduled(currentUser, team, saved);
        return mapToResponse(saved, currentUser);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listTeamMeetings(Long teamId, String filter, User currentUser) {
        projectAccessService.requireTeamAccess(teamId, currentUser);
        return applyFilter(meetingRepository.findByTeamIdWithDetails(teamId), filter).stream()
                .map(m -> mapToResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId, User currentUser) {
        Meeting meeting = requireMeeting(meetingId);
        projectAccessService.requireTeamAccess(meeting.getTeam().getTeamId(), currentUser);
        return mapToResponse(meeting, currentUser);
    }

    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, MeetingRequest request, User currentUser) {
        Meeting meeting = requireMeeting(meetingId);
        requireCanManage(meeting.getTeam(), currentUser);
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot edit a cancelled meeting");
        }
        validateTimes(request.getStartAt(), request.getEndAt());

        meeting.setTitle(request.getTitle().trim());
        meeting.setAgenda(blankToNull(request.getAgenda()));
        meeting.setStartAt(request.getStartAt());
        meeting.setEndAt(request.getEndAt());
        meeting.setLocation(blankToNull(request.getLocation()));
        meeting.setMeetingLink(blankToNull(request.getMeetingLink()));
        if (meeting.getStatus() == MeetingStatus.COMPLETED) {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }

        Meeting saved = meetingRepository.save(meeting);
        activityService.logActivity(
                currentUser,
                saved.getTeam().getProject(),
                "MEETING_UPDATED",
                displayName(currentUser) + " updated meeting \"" + saved.getTitle() + "\"",
                "MEETING",
                saved.getMeetingId()
        );
        notificationService.notifyMeetingUpdated(currentUser, saved.getTeam(), saved);
        return mapToResponse(saved, currentUser);
    }

    @Transactional
    public MeetingResponse cancelMeeting(Long meetingId, User currentUser) {
        Meeting meeting = requireMeeting(meetingId);
        requireCanManage(meeting.getTeam(), currentUser);
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            return mapToResponse(meeting, currentUser);
        }
        meeting.setStatus(MeetingStatus.CANCELLED);
        Meeting saved = meetingRepository.save(meeting);
        activityService.logActivity(
                currentUser,
                saved.getTeam().getProject(),
                "MEETING_CANCELLED",
                displayName(currentUser) + " cancelled meeting \"" + saved.getTitle() + "\"",
                "MEETING",
                saved.getMeetingId()
        );
        notificationService.notifyMeetingCancelled(currentUser, saved.getTeam(), saved);
        return mapToResponse(saved, currentUser);
    }

    @Transactional
    public MeetingResponse saveMinutes(Long meetingId, String minutes, User currentUser) {
        Meeting meeting = requireMeeting(meetingId);
        requireCanManage(meeting.getTeam(), currentUser);
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add minutes to a cancelled meeting");
        }
        meeting.setMinutes(minutes.trim());
        meeting.setStatus(MeetingStatus.COMPLETED);
        Meeting saved = meetingRepository.save(meeting);
        activityService.logActivity(
                currentUser,
                saved.getTeam().getProject(),
                "MEETING_MINUTES_SAVED",
                displayName(currentUser) + " saved minutes for \"" + saved.getTitle() + "\"",
                "MEETING",
                saved.getMeetingId()
        );
        notificationService.notifyMeetingMinutes(currentUser, saved.getTeam(), saved);
        return mapToResponse(saved, currentUser);
    }

    @Transactional
    public MeetingResponse rsvp(Long meetingId, String responseValue, User currentUser) {
        Meeting meeting = requireMeeting(meetingId);
        projectAccessService.requireTeamAccess(meeting.getTeam().getTeamId(), currentUser);
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot RSVP to a cancelled meeting");
        }
        MeetingRsvpResponse response;
        try {
            response = MeetingRsvpResponse.valueOf(responseValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RSVP. Allowed values: GOING, NOT_GOING, MAYBE");
        }

        MeetingAttendee attendee = meetingAttendeeRepository
                .findByMeeting_MeetingIdAndUser_UserId(meetingId, currentUser.getUserId())
                .orElseGet(MeetingAttendee::new);
        attendee.setMeeting(meeting);
        attendee.setUser(currentUser);
        attendee.setResponse(response);
        meetingAttendeeRepository.save(attendee);
        return mapToResponse(requireMeeting(meetingId), currentUser);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings(String filter, User currentUser) {
        List<Long> teamIds = teamMemberRepository.findActiveTeamIdsByUserId(currentUser.getUserId());
        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }
        return applyFilter(meetingRepository.findByTeamIdInWithDetails(teamIds), filter).stream()
                .map(m -> mapToResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getLecturerMeetings(String filter, User currentUser) {
        if (currentUser.getRole() != Role.LECTURER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only lecturers can view this meeting overview");
        }
        return applyFilter(meetingRepository.findByLecturerIdWithDetails(currentUser.getUserId()), filter).stream()
                .map(m -> mapToResponse(m, currentUser))
                .collect(Collectors.toList());
    }

    private Meeting requireMeeting(Long meetingId) {
        return meetingRepository.findByIdWithDetails(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    private void requireCanManage(Team team, User user) {
        if (canManage(team, user)) {
            return;
        }
        throw new UnauthorizedAccessException("Only the team leader can manage this meeting");
    }

    private boolean canManage(Team team, User user) {
        if (user == null || team == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return team.getTeamLeader() != null && team.getTeamLeader().getUserId().equals(user.getUserId());
    }

    private void validateTimes(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end time are required");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private List<Meeting> applyFilter(List<Meeting> meetings, String filter) {
        if (meetings == null) {
            return List.of();
        }
        String key = filter == null ? "all" : filter.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        if ("upcoming".equals(key)) {
            return meetings.stream()
                    .filter(m -> m.getStatus() != MeetingStatus.CANCELLED)
                    .filter(m -> m.getEndAt() != null && !m.getEndAt().isBefore(now))
                    .collect(Collectors.toList());
        }
        if ("past".equals(key)) {
            return meetings.stream()
                    .filter(m -> m.getStatus() == MeetingStatus.CANCELLED
                            || (m.getEndAt() != null && m.getEndAt().isBefore(now)))
                    .collect(Collectors.toList());
        }
        return meetings;
    }

    private MeetingResponse mapToResponse(Meeting meeting, User currentUser) {
        MeetingResponse response = new MeetingResponse();
        response.setMeetingId(meeting.getMeetingId());
        response.setTitle(meeting.getTitle());
        response.setAgenda(meeting.getAgenda());
        response.setStartAt(meeting.getStartAt());
        response.setEndAt(meeting.getEndAt());
        response.setLocation(meeting.getLocation());
        response.setMeetingLink(meeting.getMeetingLink());
        response.setMinutes(meeting.getMinutes());
        response.setStatus(meeting.getStatus() != null ? meeting.getStatus().name() : MeetingStatus.SCHEDULED.name());
        response.setCreatedAt(meeting.getCreatedAt());
        response.setUpdatedAt(meeting.getUpdatedAt());

        Team team = meeting.getTeam();
        if (team != null) {
            response.setTeamId(team.getTeamId());
            response.setTeamName(team.getName());
            if (team.getProject() != null) {
                response.setProjectId(team.getProject().getProjectId());
                response.setProjectTitle(team.getProject().getTitle());
            }
        }
        if (meeting.getCreatedBy() != null) {
            response.setCreatedBy(meeting.getCreatedBy().getUserId());
            response.setCreatedByName(displayName(meeting.getCreatedBy()));
        }
        response.setCanManage(canManage(team, currentUser));

        List<MeetingAttendee> attendees = meetingAttendeeRepository.findByMeetingIdWithUser(meeting.getMeetingId());
        if (attendees == null) {
            attendees = Collections.emptyList();
        }
        response.setAttendees(attendees.stream().map(this::mapAttendee).collect(Collectors.toList()));
        if (currentUser != null) {
            attendees.stream()
                    .filter(a -> a.getUser() != null && a.getUser().getUserId().equals(currentUser.getUserId()))
                    .findFirst()
                    .ifPresent(a -> response.setMyRsvp(a.getResponse().name()));
        }
        return response;
    }

    private MeetingAttendeeResponse mapAttendee(MeetingAttendee attendee) {
        MeetingAttendeeResponse response = new MeetingAttendeeResponse();
        response.setAttendeeId(attendee.getAttendeeId());
        response.setResponse(attendee.getResponse() != null ? attendee.getResponse().name() : null);
        response.setRespondedAt(attendee.getRespondedAt());
        if (attendee.getUser() != null) {
            response.setUserId(attendee.getUser().getUserId());
            response.setUserName(displayName(attendee.getUser()));
        }
        return response;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String displayName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
