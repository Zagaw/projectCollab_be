package com.example.projectCollab.insights;

import com.example.projectCollab.entity.Milestone;
import com.example.projectCollab.entity.Task;
import com.example.projectCollab.entity.TaskStatus;
import com.example.projectCollab.entity.Team;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RiskEngine {

    public TeamRiskAssessment assess(Team team, List<Task> tasks, List<Milestone> milestones, LocalDateTime now) {
        List<Task> safeTasks = tasks == null ? List.of() : tasks;
        List<Milestone> safeMilestones = milestones == null ? List.of() : milestones;

        List<Task> overdueTasks = safeTasks.stream()
                .filter(task -> DeadlineRules.isTaskOverdue(task, now))
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Task> dueSoonTasks = safeTasks.stream()
                .filter(task -> DeadlineRules.isTaskDueSoon(task, now))
                .sorted(Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Milestone> overdueMilestones = safeMilestones.stream()
                .filter(milestone -> DeadlineRules.isMilestoneOverdue(milestone, now))
                .sorted(Comparator.comparing(Milestone::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Milestone> dueSoonMilestones = safeMilestones.stream()
                .filter(milestone -> DeadlineRules.isMilestoneDueSoon(milestone, now))
                .sorted(Comparator.comparing(Milestone::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long taskTotal = safeTasks.size();
        long taskCompleted = safeTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();

        int score = 0;
        score += (int) Math.min(40, overdueTasks.size() * 8L);
        score += (int) Math.min(16, dueSoonTasks.size() * 4L);
        score += (int) Math.min(36, overdueMilestones.size() * 12L);
        if (taskTotal > 0 && taskCompleted == 0) {
            score += 8;
        }
        score = Math.min(100, score);

        List<String> reasons = new ArrayList<>();
        if (!overdueTasks.isEmpty()) {
            reasons.add(countLabel(overdueTasks.size(), "overdue task", "overdue tasks"));
        }
        if (!dueSoonTasks.isEmpty()) {
            reasons.add(countLabel(dueSoonTasks.size(), "task due within 48 hours", "tasks due within 48 hours"));
        }
        int namedMilestones = 0;
        for (Milestone milestone : overdueMilestones) {
            if (namedMilestones >= 3) {
                break;
            }
            reasons.add(milestoneLateReason(milestone, now));
            namedMilestones += 1;
        }
        if (overdueMilestones.size() > 3) {
            reasons.add((overdueMilestones.size() - 3) + " more overdue milestones");
        }
        if (!dueSoonMilestones.isEmpty()) {
            reasons.add(countLabel(dueSoonMilestones.size(), "milestone due within 48 hours", "milestones due within 48 hours"));
        }
        if (taskTotal > 0 && taskCompleted == 0) {
            reasons.add("No tasks completed yet");
        }
        if (reasons.isEmpty()) {
            reasons.add("No deadline risks right now");
        }

        TeamRiskAssessment assessment = new TeamRiskAssessment();
        assessment.setTeam(team);
        assessment.setScore(score);
        assessment.setBand(bandFor(score));
        assessment.setReasons(reasons);
        assessment.setTaskTotal(taskTotal);
        assessment.setTaskCompleted(taskCompleted);
        assessment.setOverdueTaskCount(overdueTasks.size());
        assessment.setDueSoonTaskCount(dueSoonTasks.size());
        assessment.setOverdueMilestoneCount(overdueMilestones.size());
        assessment.setDueSoonMilestoneCount(dueSoonMilestones.size());
        assessment.setOverdueTasks(overdueTasks);
        assessment.setDueSoonTasks(dueSoonTasks);
        assessment.setOverdueMilestones(overdueMilestones);
        assessment.setDueSoonMilestones(dueSoonMilestones);
        return assessment;
    }

    public static String bandFor(int score) {
        if (score >= 50) {
            return TeamRiskAssessment.AT_RISK;
        }
        if (score >= 25) {
            return TeamRiskAssessment.WATCH;
        }
        return TeamRiskAssessment.HEALTHY;
    }

    private String countLabel(long count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private String milestoneLateReason(Milestone milestone, LocalDateTime now) {
        String title = milestone.getTitle() == null ? "Untitled milestone" : milestone.getTitle();
        if (milestone.getDeadline() == null) {
            return "Milestone \"" + title + "\" is overdue";
        }
        long days = ChronoUnit.DAYS.between(milestone.getDeadline().toLocalDate(), now.toLocalDate());
        if (days <= 0) {
            return "Milestone \"" + title + "\" is overdue";
        }
        return "Milestone \"" + title + "\" is " + days + (days == 1 ? " day" : " days") + " late";
    }
}
