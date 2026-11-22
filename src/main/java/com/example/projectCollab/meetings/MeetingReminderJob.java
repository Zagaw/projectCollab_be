package com.example.projectCollab.meetings;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingReminderJob {

    private final MeetingReminderService meetingReminderService;

    @Scheduled(cron = "0 * * * * *")
    public void everyMinute() {
        meetingReminderService.sendDueReminders();
    }
}
