package com.example.projectCollab.service;

import com.example.projectCollab.dto.*;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.ActivityRepository;
import com.example.projectCollab.repository.MeetingRepository;
import com.example.projectCollab.repository.MilestoneRepository;
import com.example.projectCollab.repository.TaskRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int EVENT_LIMIT = 100;
    private static final int LIST_LIMIT = 50;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProjectAccessService projectAccessService;
    private final ProgressService progressService;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final MeetingRepository meetingRepository;
    private final ActivityRepository activityRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public ReportResponse generate(String typeValue, Long projectId, Long teamId,
                                   LocalDateTime from, LocalDateTime to, User currentUser) {
        String type = normalizeType(typeValue);
        Scope scope = resolveScope(projectId, teamId, currentUser);
        LocalDateTime[] period = resolvePeriod(type, from, to);

        ReportResponse report = baseReport(type, scope, period[0], period[1]);
        switch (type) {
            case "PROGRESS" -> fillProgress(report, scope, currentUser);
            case "TASKS" -> fillTasks(report, scope, currentUser);
            case "CONTRIBUTIONS" -> fillContributions(report, scope, currentUser);
            case "WEEKLY" -> fillWeekly(report, scope, period[0], period[1]);
            default -> throw new IllegalArgumentException("Unknown report type");
        }
        return report;
    }

    private String normalizeType(String typeValue) {
        if (typeValue == null || typeValue.isBlank()) {
            throw new IllegalArgumentException("Report type is required");
        }
        String type = typeValue.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PROGRESS", "TASKS", "CONTRIBUTIONS", "WEEKLY").contains(type)) {
            throw new IllegalArgumentException("Allowed types: PROGRESS, TASKS, CONTRIBUTIONS, WEEKLY");
        }
        return type;
    }

    private LocalDateTime[] resolvePeriod(String type, LocalDateTime from, LocalDateTime to) {
        if (!"WEEKLY".equals(type)) {
            return new LocalDateTime[]{from, to};
        }
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(7);
        if (!end.isAfter(start) && !end.isEqual(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        return new LocalDateTime[]{start, end};
    }

    private Scope resolveScope(Long projectId, Long teamId, User currentUser) {
        if (teamId != null) {
            projectAccessService.requireTeamAccess(teamId, currentUser);
            Team team = projectAccessService.requireTeam(teamId);
            if (projectId != null && team.getProject() != null
                    && !team.getProject().getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("Team does not belong to this project");
            }
            return Scope.team(team);
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Select a project or team");
        }
        if (currentUser.getRole() != Role.LECTURER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Select one of your teams to generate a report");
        }
        projectAccessService.requireProjectAccess(projectId, currentUser);
        return Scope.project(projectAccessService.requireProject(projectId));
    }

    private ReportResponse baseReport(String type, Scope scope, LocalDateTime from, LocalDateTime to) {
        ReportResponse report = new ReportResponse();
        report.setType(type);
        report.setGeneratedAt(LocalDateTime.now());
        report.setPeriodStart(from);
        report.setPeriodEnd(to);
        report.setProjectId(scope.projectId);
        report.setProjectTitle(scope.projectTitle);
        report.setTeamId(scope.teamId);
        report.setTeamName(scope.teamName);
        report.setTitle(titleFor(type, scope));
        return report;
    }

    private String titleFor(String type, Scope scope) {
        String scopeName = scope.teamName != null ? scope.teamName : scope.projectTitle;
        return switch (type) {
            case "PROGRESS" -> "Progress summary — " + scopeName;
            case "TASKS" -> "Task completion report — " + scopeName;
            case "CONTRIBUTIONS" -> "Member contribution report — " + scopeName;
            case "WEEKLY" -> "Weekly activity report — " + scopeName;
            default -> "Report — " + scopeName;
        };
    }

    private void fillProgress(ReportResponse report, Scope scope, User currentUser) {
        if (scope.teamId != null) {
            TeamProgressResponse progress = progressService.getTeamProgress(scope.teamId, currentUser);
            report.getMetrics().add(metric("Task completion", progress.getTaskPercent() + "%"));
            report.getMetrics().add(metric("Tasks completed", progress.getTaskCompleted() + " / " + progress.getTaskTotal()));
            report.getMetrics().add(metric("Overdue tasks", String.valueOf(progress.getTaskOverdue())));
            report.getMetrics().add(metric("Milestone completion", progress.getMilestonePercent() + "%"));
            report.getMetrics().add(metric("Milestones completed",
                    progress.getMilestoneCompleted() + " / " + progress.getMilestoneTotal()));
        } else {
            ProjectProgressResponse progress = progressService.getProjectProgress(scope.projectId, currentUser);
            report.getMetrics().add(metric("Task completion", progress.getTaskPercent() + "%"));
            report.getMetrics().add(metric("Tasks completed", progress.getTaskCompleted() + " / " + progress.getTaskTotal()));
            report.getMetrics().add(metric("Overdue tasks", String.valueOf(progress.getTaskOverdue())));
            report.getMetrics().add(metric("Milestone completion", progress.getMilestonePercent() + "%"));
            report.getMetrics().add(metric("Teams", String.valueOf(progress.getTeamCount())));
            report.getMetrics().add(metric("Teams with overdue tasks", String.valueOf(progress.getOverdueTeamCount())));
            ReportSection teams = section("Teams", List.of("Team", "Task %", "Tasks", "Overdue", "Milestone %"));
            for (TeamProgressResponse team : progress.getTeams()) {
                teams.getRows().add(List.of(
                        safe(team.getTeamName()),
                        team.getTaskPercent() + "%",
                        team.getTaskCompleted() + "/" + team.getTaskTotal(),
                        String.valueOf(team.getTaskOverdue()),
                        team.getMilestonePercent() + "%"
                ));
            }
            report.getSections().add(teams);
        }
    }

    private void fillTasks(ReportResponse report, Scope scope, User currentUser) {
        List<Task> tasks = loadTasks(scope);
        long todo = countStatus(tasks, TaskStatus.TODO);
        long inProgress = countStatus(tasks, TaskStatus.IN_PROGRESS);
        long review = countStatus(tasks, TaskStatus.REVIEW);
        long blocked = countStatus(tasks, TaskStatus.BLOCKED);
        long completed = countStatus(tasks, TaskStatus.COMPLETED);
        long overdue = tasks.stream().filter(this::isOverdue).count();

        report.getMetrics().add(metric("Total tasks", String.valueOf(tasks.size())));
        report.getMetrics().add(metric("Completed", String.valueOf(completed)));
        report.getMetrics().add(metric("In progress", String.valueOf(inProgress)));
        report.getMetrics().add(metric("Review", String.valueOf(review)));
        report.getMetrics().add(metric("To do", String.valueOf(todo)));
        report.getMetrics().add(metric("Blocked", String.valueOf(blocked)));
        report.getMetrics().add(metric("Overdue", String.valueOf(overdue)));

        if (scope.teamId == null) {
            ProjectProgressResponse progress = progressService.getProjectProgress(scope.projectId, currentUser);
            ReportSection teams = section("Completion by team", List.of("Team", "Completed", "Total", "Percent", "Overdue"));
            for (TeamProgressResponse team : progress.getTeams()) {
                teams.getRows().add(List.of(
                        safe(team.getTeamName()),
                        String.valueOf(team.getTaskCompleted()),
                        String.valueOf(team.getTaskTotal()),
                        team.getTaskPercent() + "%",
                        String.valueOf(team.getTaskOverdue())
                ));
            }
            report.getSections().add(teams);
        }

        ReportSection completedSection = section("Completed tasks",
                List.of("Title", "Assignee", "Team", "Completed at"));
        tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .limit(LIST_LIMIT)
                .forEach(task -> completedSection.getRows().add(List.of(
                        safe(task.getTitle()),
                        assigneeName(task),
                        teamName(task),
                        format(task.getCompletedAt())
                )));
        report.getSections().add(completedSection);

        ReportSection overdueSection = section("Overdue tasks",
                List.of("Title", "Assignee", "Team", "Deadline", "Status"));
        tasks.stream()
                .filter(this::isOverdue)
                .limit(LIST_LIMIT)
                .forEach(task -> overdueSection.getRows().add(List.of(
                        safe(task.getTitle()),
                        assigneeName(task),
                        teamName(task),
                        format(task.getDeadline()),
                        task.getStatus() != null ? task.getStatus().name() : ""
                )));
        report.getSections().add(overdueSection);
    }

    private void fillContributions(ReportResponse report, Scope scope, User currentUser) {
        List<MemberContributionResponse> members;
        if (scope.teamId != null) {
            TeamProgressResponse progress = progressService.getTeamProgress(scope.teamId, currentUser);
            report.getMetrics().add(metric("Members", String.valueOf(progress.getMembers().size())));
            report.getMetrics().add(metric("Tasks completed (team)",
                    progress.getTaskCompleted() + " / " + progress.getTaskTotal()));
            members = progress.getMembers();
        } else {
            ProjectProgressResponse progress = progressService.getProjectProgress(scope.projectId, currentUser);
            members = new ArrayList<>();
            ReportSection byTeam = section("Contributions by team",
                    List.of("Team", "Member", "Assigned", "Completed", "Overdue", "Comments", "Files", "Discussions"));
            for (TeamProgressResponse team : progress.getTeams()) {
                for (MemberContributionResponse member : team.getMembers()) {
                    members.add(member);
                    byTeam.getRows().add(List.of(
                            safe(team.getTeamName()),
                            safe(member.getName()),
                            String.valueOf(member.getAssigned()),
                            String.valueOf(member.getCompleted()),
                            String.valueOf(member.getOverdue()),
                            String.valueOf(member.getComments()),
                            String.valueOf(member.getFiles()),
                            String.valueOf(member.getDiscussions())
                    ));
                }
            }
            report.getMetrics().add(metric("Teams", String.valueOf(progress.getTeamCount())));
            report.getSections().add(byTeam);
            return;
        }

        ReportSection table = section("Member contributions",
                List.of("Member", "Assigned", "Completed", "Overdue", "Comments", "Files", "Discussions"));
        for (MemberContributionResponse member : members) {
            table.getRows().add(List.of(
                    safe(member.getName()),
                    String.valueOf(member.getAssigned()),
                    String.valueOf(member.getCompleted()),
                    String.valueOf(member.getOverdue()),
                    String.valueOf(member.getComments()),
                    String.valueOf(member.getFiles()),
                    String.valueOf(member.getDiscussions())
            ));
        }
        report.getSections().add(table);
    }

    private void fillWeekly(ReportResponse report, Scope scope, LocalDateTime from, LocalDateTime to) {
        List<Task> tasks = loadTasks(scope).stream()
                .filter(t -> t.getCompletedAt() != null && inRange(t.getCompletedAt(), from, to))
                .toList();
        List<Milestone> milestones = loadMilestones(scope).stream()
                .filter(m -> m.getCompletedAt() != null && inRange(m.getCompletedAt(), from, to))
                .toList();
        List<Meeting> meetings = loadMeetings(scope);
        long createdMeetings = meetings.stream().filter(m -> inRange(m.getCreatedAt(), from, to)).count();
        long cancelledMeetings = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.CANCELLED && inRange(m.getUpdatedAt(), from, to))
                .count();
        List<Activity> activities = loadActivities(scope, from, to);

        report.getMetrics().add(metric("Period", format(from) + " – " + format(to)));
        report.getMetrics().add(metric("Tasks completed in period", String.valueOf(tasks.size())));
        report.getMetrics().add(metric("Milestones completed in period", String.valueOf(milestones.size())));
        report.getMetrics().add(metric("Meetings scheduled", String.valueOf(createdMeetings)));
        report.getMetrics().add(metric("Meetings cancelled", String.valueOf(cancelledMeetings)));
        report.getMetrics().add(metric("Activity events", String.valueOf(activities.size())));

        ReportSection taskSection = section("Tasks completed", List.of("Title", "Assignee", "Team", "Completed at"));
        tasks.stream().limit(LIST_LIMIT).forEach(task -> taskSection.getRows().add(List.of(
                safe(task.getTitle()),
                assigneeName(task),
                teamName(task),
                format(task.getCompletedAt())
        )));
        report.getSections().add(taskSection);

        ReportSection milestoneSection = section("Milestones completed", List.of("Title", "Team", "Completed at"));
        milestones.stream().limit(LIST_LIMIT).forEach(milestone -> milestoneSection.getRows().add(List.of(
                safe(milestone.getTitle()),
                milestone.getTeam() != null ? safe(milestone.getTeam().getName()) : "",
                format(milestone.getCompletedAt())
        )));
        report.getSections().add(milestoneSection);

        ReportSection meetingSection = section("Meetings in period", List.of("Title", "Team", "Status", "When"));
        meetings.stream()
                .filter(m -> inRange(m.getCreatedAt(), from, to) || (m.getStatus() == MeetingStatus.CANCELLED && inRange(m.getUpdatedAt(), from, to)))
                .limit(LIST_LIMIT)
                .forEach(meeting -> meetingSection.getRows().add(List.of(
                        safe(meeting.getTitle()),
                        meeting.getTeam() != null ? safe(meeting.getTeam().getName()) : "",
                        meeting.getStatus() != null ? meeting.getStatus().name() : "",
                        format(meeting.getStartAt())
                )));
        report.getSections().add(meetingSection);

        ReportSection activitySection = section("Activity log", List.of("When", "Member", "Action", "Description"));
        activities.stream().limit(EVENT_LIMIT).forEach(activity -> activitySection.getRows().add(List.of(
                format(activity.getCreatedAt()),
                activity.getUser() != null
                        ? safe(activity.getUser().getFirstName() + " " + activity.getUser().getLastName())
                        : "",
                safe(activity.getAction()),
                safe(activity.getDescription())
        )));
        report.getSections().add(activitySection);
    }

    private List<Task> loadTasks(Scope scope) {
        if (scope.teamId != null) {
            List<Task> tasks = taskRepository.findByTeamIdWithAssignee(scope.teamId);
            return tasks != null ? tasks : List.of();
        }
        List<Task> tasks = taskRepository.findByProjectIdWithAssignee(scope.projectId);
        return tasks != null ? tasks : List.of();
    }

    private List<Milestone> loadMilestones(Scope scope) {
        if (scope.teamId != null) {
            List<Milestone> milestones = milestoneRepository.findByTeam_TeamId(scope.teamId);
            return milestones != null ? milestones : List.of();
        }
        List<Milestone> all = new ArrayList<>();
        for (Team team : teamsForProject(scope.projectId)) {
            List<Milestone> milestones = milestoneRepository.findByTeam_TeamId(team.getTeamId());
            if (milestones != null) {
                all.addAll(milestones);
            }
        }
        return all;
    }

    private List<Meeting> loadMeetings(Scope scope) {
        if (scope.teamId != null) {
            List<Meeting> meetings = meetingRepository.findByTeamIdWithDetails(scope.teamId);
            return meetings != null ? meetings : List.of();
        }
        List<Long> teamIds = teamsForProject(scope.projectId).stream().map(Team::getTeamId).toList();
        if (teamIds.isEmpty()) {
            return List.of();
        }
        List<Meeting> meetings = meetingRepository.findByTeamIdInWithDetails(teamIds);
        return meetings != null ? meetings : List.of();
    }

    private List<Activity> loadActivities(Scope scope, LocalDateTime from, LocalDateTime to) {
        if (scope.projectId == null) {
            return List.of();
        }
        List<Activity> activities;
        if (scope.teamId != null) {
            List<Long> userIds = teamMemberRepository.findActiveMembersByTeamId(scope.teamId).stream()
                    .map(member -> member.getUser().getUserId())
                    .toList();
            if (userIds.isEmpty()) {
                return List.of();
            }
            activities = activityRepository.findByProjectUsersAndCreatedAtBetween(
                    scope.projectId, userIds, from, to);
        } else {
            activities = activityRepository.findByProjectAndCreatedAtBetween(scope.projectId, from, to);
        }
        return activities != null ? activities : List.of();
    }

    private List<Team> teamsForProject(Long projectId) {
        List<Team> teams = teamRepository.findByProject_ProjectId(projectId);
        return teams != null ? teams : List.of();
    }

    private boolean isOverdue(Task task) {
        return task.getStatus() != TaskStatus.COMPLETED
                && task.getDeadline() != null
                && task.getDeadline().isBefore(LocalDateTime.now());
    }

    private long countStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(t -> t.getStatus() == status).count();
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        return value != null && !value.isBefore(from) && !value.isAfter(to);
    }

    private ReportMetric metric(String label, String value) {
        return new ReportMetric(label, value);
    }

    private ReportSection section(String title, List<String> columns) {
        ReportSection section = new ReportSection();
        section.setTitle(title);
        section.setColumns(columns);
        section.setRows(new ArrayList<>());
        return section;
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String assigneeName(Task task) {
        if (task.getAssignedTo() == null) {
            return "Unassigned";
        }
        return task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName();
    }

    private String teamName(Task task) {
        return task.getTeam() != null ? safe(task.getTeam().getName()) : "";
    }

    private static class Scope {
        private final Long projectId;
        private final String projectTitle;
        private final Long teamId;
        private final String teamName;

        private Scope(Long projectId, String projectTitle, Long teamId, String teamName) {
            this.projectId = projectId;
            this.projectTitle = projectTitle;
            this.teamId = teamId;
            this.teamName = teamName;
        }

        static Scope team(Team team) {
            Project project = team.getProject();
            return new Scope(
                    project != null ? project.getProjectId() : null,
                    project != null ? project.getTitle() : null,
                    team.getTeamId(),
                    team.getName()
            );
        }

        static Scope project(Project project) {
            return new Scope(project.getProjectId(), project.getTitle(), null, null);
        }
    }
}
