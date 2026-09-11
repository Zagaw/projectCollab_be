package com.example.projectCollab.insights;

import com.example.projectCollab.dto.ScanResultResponse;
import com.example.projectCollab.entity.Milestone;
import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.Task;
import com.example.projectCollab.entity.Team;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.InsightService;
import com.example.projectCollab.service.NotificationService;
import com.example.projectCollab.repository.MilestoneRepository;
import com.example.projectCollab.repository.TaskRepository;
import com.example.projectCollab.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskReminderService {

    private static final Logger log = LoggerFactory.getLogger(RiskReminderService.class);

    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final InsightService insightService;

    @Transactional
    public ScanResultResponse scanNow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusHours(DeadlineRules.DUE_SOON_HOURS);
        LocalDateTime since = now.minusHours(24);
        int created = 0;

        List<Task> overdueTasks = taskRepository.findOverdueTasksWithRelations(now);
        for (Task task : overdueTasks) {
            created += notifyTask(task, "TASK_OVERDUE", "Task overdue",
                    "The task \"" + title(task.getTitle()) + "\" is overdue.", since);
        }

        List<Task> dueSoonTasks = taskRepository.findDueSoonTasksWithRelations(now, until);
        for (Task task : dueSoonTasks) {
            created += notifyTask(task, "TASK_DUE_SOON", "Task due soon",
                    "The task \"" + title(task.getTitle()) + "\" is due by " + when(task.getDeadline()) + ".", since);
        }

        List<Milestone> overdueMilestones = milestoneRepository.findOverdueMilestonesWithRelations(now);
        for (Milestone milestone : overdueMilestones) {
            created += notifyMilestone(milestone, "MILESTONE_OVERDUE", "Milestone overdue",
                    "The milestone \"" + title(milestone.getTitle()) + "\" is overdue.", since);
        }

        List<Milestone> dueSoonMilestones = milestoneRepository.findDueSoonMilestonesWithRelations(now, until);
        for (Milestone milestone : dueSoonMilestones) {
            created += notifyMilestone(milestone, "MILESTONE_DUE_SOON", "Milestone due soon",
                    "The milestone \"" + title(milestone.getTitle()) + "\" is due by "
                            + when(milestone.getDeadline()) + ".", since);
        }

        List<Team> teams = teamRepository.findAllWithProjectAndLeader();
        for (Team team : teams) {
            TeamRiskAssessment assessment = insightService.assess(team, now);
            if (!assessment.isAtRisk()) {
                continue;
            }
            String message = title(team.getName()) + " is At risk (" + assessment.getScore() + "). "
                    + (assessment.getReasons().isEmpty() ? "" : assessment.getReasons().get(0) + ".");
            Long projectId = team.getProject() != null ? team.getProject().getProjectId() : null;
            Map<Long, User> recipients = new LinkedHashMap<>();
            if (team.getTeamLeader() != null) {
                recipients.put(team.getTeamLeader().getUserId(), team.getTeamLeader());
            }
            if (team.getProject() != null && team.getProject().getLecturer() != null) {
                User lecturer = team.getProject().getLecturer();
                recipients.put(lecturer.getUserId(), lecturer);
            }
            for (User recipient : recipients.values()) {
                if (notificationService.notifyIfAbsent(
                        recipient,
                        "TEAM_AT_RISK",
                        "Team at risk",
                        message,
                        "TEAM",
                        team.getTeamId(),
                        projectId,
                        since)) {
                    created += 1;
                }
            }
        }

        log.info("Deadline risk scan created {} notifications", created);
        return new ScanResultResponse(created);
    }

    private int notifyTask(Task task, String type, String title, String message, LocalDateTime since) {
        User recipient = taskRecipient(task);
        Team team = task.getTeam();
        Project project = team != null ? team.getProject() : task.getProject();
        Long projectId = project != null ? project.getProjectId() : null;
        return notificationService.notifyIfAbsent(
                recipient, type, title, message, "TASK", task.getTaskId(), projectId, since) ? 1 : 0;
    }

    private int notifyMilestone(Milestone milestone, String type, String title, String message, LocalDateTime since) {
        Team team = milestone.getTeam();
        if (team == null) {
            return 0;
        }
        User recipient = team.getTeamLeader();
        if (recipient == null && team.getProject() != null) {
            recipient = team.getProject().getLecturer();
        }
        Long projectId = team.getProject() != null ? team.getProject().getProjectId() : null;
        return notificationService.notifyIfAbsent(
                recipient, type, title, message, "MILESTONE", milestone.getMilestoneId(), projectId, since) ? 1 : 0;
    }

    private User taskRecipient(Task task) {
        if (task.getAssignedTo() != null) {
            return task.getAssignedTo();
        }
        if (task.getTeam() != null && task.getTeam().getTeamLeader() != null) {
            return task.getTeam().getTeamLeader();
        }
        Project project = task.getTeam() != null ? task.getTeam().getProject() : task.getProject();
        return project != null ? project.getLecturer() : null;
    }

    private String title(String value) {
        return value == null || value.isBlank() ? "Untitled" : value;
    }

    private String when(LocalDateTime deadline) {
        return deadline == null ? "soon" : deadline.toLocalDate().toString();
    }
}
