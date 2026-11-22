package com.example.projectCollab.meetings;

import com.example.projectCollab.entity.Meeting;
import com.example.projectCollab.entity.MeetingRsvpResponse;
import com.example.projectCollab.entity.MeetingStatus;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.repository.MeetingAttendeeRepository;
import com.example.projectCollab.repository.MeetingRepository;
import com.example.projectCollab.service.NotificationService;
import com.example.projectCollab.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingReminderService {

    public static final int LEAD_MINUTES = 15;

    private static final Logger log = LoggerFactory.getLogger(MeetingReminderService.class);

    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final ProjectAccessService projectAccessService;
    private final NotificationService notificationService;

    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusMinutes(LEAD_MINUTES);
        List<Meeting> meetings = meetingRepository.findScheduledStartingBetween(
                MeetingStatus.SCHEDULED, now, until);

        int created = 0;
        for (Meeting meeting : meetings) {
            created += remindTeam(meeting, now);
        }
        if (created > 0) {
            log.info("Sent {} meeting reminder notifications for {} upcoming meeting(s)",
                    created, meetings.size());
        }
        return created;
    }

    private int remindTeam(Meeting meeting, LocalDateTime now) {
        if (meeting.getTeam() == null) {
            return 0;
        }
        List<User> members = projectAccessService.getActiveTeamMembers(meeting.getTeam().getTeamId());
        Set<Long> declined = declinedUserIds(meeting.getMeetingId());
        List<User> recipients = members.stream()
                .filter(user -> user != null && !declined.contains(user.getUserId()))
                .collect(Collectors.toList());
        LocalDateTime since = meeting.getStartAt() != null
                ? meeting.getStartAt().minusMinutes(LEAD_MINUTES + 5)
                : now.minusHours(1);
        return notificationService.notifyMeetingStartingSoon(meeting, recipients, since);
    }

    private Set<Long> declinedUserIds(Long meetingId) {
        return meetingAttendeeRepository.findByMeetingIdWithUser(meetingId).stream()
                .filter(attendee -> attendee.getResponse() == MeetingRsvpResponse.NOT_GOING)
                .map(attendee -> attendee.getUser().getUserId())
                .collect(Collectors.toSet());
    }
}
