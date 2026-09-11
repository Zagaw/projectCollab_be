package com.example.projectCollab.service;

import com.example.projectCollab.dto.DeadlineItemResponse;
import com.example.projectCollab.dto.MyInsightResponse;
import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.TeamRiskResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.insights.DeadlineRules;
import com.example.projectCollab.insights.RiskEngine;
import com.example.projectCollab.insights.RiskSummaryGenerator;
import com.example.projectCollab.insights.TeamRiskAssessment;
import com.example.projectCollab.repository.MilestoneRepository;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.TaskRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final ProjectAccessService projectAccessService;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final RiskEngine riskEngine;
    private final RiskSummaryGenerator riskSummaryGenerator;

    @Transactional(readOnly = true)
    public MyInsightResponse getMyInsights(User currentUser) {
        LocalDateTime now = LocalDateTime.now();
        MyInsightResponse response = new MyInsightResponse();

        List<Task> assigned = taskRepository.findByAssignedToUserId(currentUser.getUserId());
        if (assigned == null) {
            assigned = List.of();
        }
        List<DeadlineItemResponse> overdue = new ArrayList<>();
        List<DeadlineItemResponse> dueSoon = new ArrayList<>();
        for (Task task : assigned) {
            if (DeadlineRules.isTaskOverdue(task, now)) {
                overdue.add(toItem(task, true));
            } else if (DeadlineRules.isTaskDueSoon(task, now)) {
                dueSoon.add(toItem(task, false));
            }
        }
        overdue.sort(Comparator.comparing(DeadlineItemResponse::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
        dueSoon.sort(Comparator.comparing(DeadlineItemResponse::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
        response.setMyOverdue(overdue);
        response.setMyDueSoon(dueSoon);

        List<TeamMember> memberships = teamMemberRepository.findActiveTeamsByUserId(currentUser.getUserId());
        List<TeamRiskResponse> teams = new ArrayList<>();
        for (TeamMember membership : memberships) {
            teams.add(toResponse(assess(membership.getTeam(), now), false));
        }
        teams.sort(Comparator.comparingInt(TeamRiskResponse::getScore).reversed()
                .thenComparing(TeamRiskResponse::getTeamName, Comparator.nullsLast(String::compareToIgnoreCase)));
        response.setTeams(teams);
        return response;
    }

    @Transactional(readOnly = true)
    public List<TeamRiskResponse> getLecturerInsights(User currentUser) {
        if (currentUser.getRole() != Role.LECTURER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only lecturers can view this insights overview");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Project> projects = currentUser.getRole() == Role.ADMIN
                ? projectRepository.findAll()
                : projectRepository.findByLecturer_UserId(currentUser.getUserId());
        List<TeamRiskResponse> teams = new ArrayList<>();
        for (Project project : projects) {
            List<Team> projectTeams = teamRepository.findByProject_ProjectId(project.getProjectId());
            for (Team team : projectTeams) {
                teams.add(toResponse(assess(team, now), false));
            }
        }
        teams.sort(Comparator.comparingInt(TeamRiskResponse::getScore).reversed()
                .thenComparing(TeamRiskResponse::getTeamName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return teams;
    }

    @Transactional(readOnly = true)
    public TeamRiskResponse getTeamInsight(Long teamId, User currentUser) {
        projectAccessService.requireTeamAccess(teamId, currentUser);
        Team team = projectAccessService.requireTeam(teamId);
        return toResponse(assess(team, LocalDateTime.now()), true);
    }

    @Transactional(readOnly = true)
    public RiskSummaryResponse summarizeTeam(Long teamId, User currentUser) {
        TeamRiskResponse risk = getTeamInsight(teamId, currentUser);
        return riskSummaryGenerator.generate(risk);
    }

    public TeamRiskAssessment assess(Team team, LocalDateTime now) {
        List<Task> tasks = taskRepository.findByTeamIdWithAssignee(team.getTeamId());
        List<Milestone> milestones = milestoneRepository.findByTeam_TeamId(team.getTeamId());
        return riskEngine.assess(team, tasks, milestones, now);
    }

    public TeamRiskResponse toResponse(TeamRiskAssessment assessment, boolean includeItems) {
        Team team = assessment.getTeam();
        TeamRiskResponse response = new TeamRiskResponse();
        if (team != null) {
            response.setTeamId(team.getTeamId());
            response.setTeamName(team.getName());
            if (team.getProject() != null) {
                response.setProjectId(team.getProject().getProjectId());
                response.setProjectTitle(team.getProject().getTitle());
            }
        }
        response.setScore(assessment.getScore());
        response.setBand(assessment.getBand());
        response.setReasons(assessment.getReasons());
        response.setOverdueTaskCount(assessment.getOverdueTaskCount());
        response.setDueSoonTaskCount(assessment.getDueSoonTaskCount());
        response.setOverdueMilestoneCount(assessment.getOverdueMilestoneCount());
        response.setDueSoonMilestoneCount(assessment.getDueSoonMilestoneCount());
        response.setTaskTotal(assessment.getTaskTotal());
        response.setTaskCompleted(assessment.getTaskCompleted());
        if (includeItems) {
            List<DeadlineItemResponse> overdueItems = new ArrayList<>();
            assessment.getOverdueTasks().forEach(task -> overdueItems.add(toItem(task, true)));
            assessment.getOverdueMilestones().forEach(milestone -> overdueItems.add(toItem(milestone, true)));
            List<DeadlineItemResponse> dueSoonItems = new ArrayList<>();
            assessment.getDueSoonTasks().forEach(task -> dueSoonItems.add(toItem(task, false)));
            assessment.getDueSoonMilestones().forEach(milestone -> dueSoonItems.add(toItem(milestone, false)));
            overdueItems.sort(Comparator.comparing(DeadlineItemResponse::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
            dueSoonItems.sort(Comparator.comparing(DeadlineItemResponse::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())));
            response.setOverdueItems(overdueItems);
            response.setDueSoonItems(dueSoonItems);
        }
        return response;
    }

    private DeadlineItemResponse toItem(Task task, boolean overdue) {
        DeadlineItemResponse item = new DeadlineItemResponse();
        item.setEntityType("TASK");
        item.setEntityId(task.getTaskId());
        item.setTitle(task.getTitle());
        item.setDeadline(task.getDeadline());
        item.setOverdue(overdue);
        Team team = task.getTeam();
        if (team != null) {
            item.setTeamId(team.getTeamId());
            item.setTeamName(team.getName());
            if (team.getProject() != null) {
                item.setProjectId(team.getProject().getProjectId());
                item.setProjectTitle(team.getProject().getTitle());
            }
        }
        if (item.getProjectId() == null && task.getProject() != null) {
            item.setProjectId(task.getProject().getProjectId());
            item.setProjectTitle(task.getProject().getTitle());
        }
        return item;
    }

    private DeadlineItemResponse toItem(Milestone milestone, boolean overdue) {
        DeadlineItemResponse item = new DeadlineItemResponse();
        item.setEntityType("MILESTONE");
        item.setEntityId(milestone.getMilestoneId());
        item.setTitle(milestone.getTitle());
        item.setDeadline(milestone.getDeadline());
        item.setOverdue(overdue);
        Team team = milestone.getTeam();
        if (team != null) {
            item.setTeamId(team.getTeamId());
            item.setTeamName(team.getName());
            if (team.getProject() != null) {
                item.setProjectId(team.getProject().getProjectId());
                item.setProjectTitle(team.getProject().getTitle());
            }
        }
        return item;
    }
}
