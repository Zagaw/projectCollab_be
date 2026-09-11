package com.example.projectCollab.insights;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskReminderJob {

    private final RiskReminderService riskReminderService;

    @Scheduled(cron = "0 0 * * * *")
    public void hourlyScan() {
        riskReminderService.scanNow();
    }
}
