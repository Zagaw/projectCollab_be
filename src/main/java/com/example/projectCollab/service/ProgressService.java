package com.example.projectCollab.service;

import com.example.projectCollab.dto.MemberContributionResponse;
import com.example.projectCollab.dto.ProjectProgressResponse;
import com.example.projectCollab.dto.TeamProgressResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.insights.DeadlineRules;
import com.example.projectCollab.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProjectAccessService projectAccessService;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final DiscussionRepository discussionRepository;
    private final DiscussionReplyRepository discussionReplyRepository;

    @Transactional(readOnly = true)
    public TeamProgressResponse getTeamProgress(Long teamId, User currentUser) {
        projectAccessService.requireTeamAccess(teamId, currentUser);
        Team team = projectAccessService.requireTeam(teamId);
        return buildTeamProgress(team, currentUser, true);
    }

    @Transactional(readOnly = true)
    public ProjectProgressResponse getProjectProgress(Long projectId, User currentUser) {
        projectAccessService.requireProjectAccess(projectId, currentUser);
        Project project = projectAccessService.requireProject(projectId);
        return buildProjectProgress(project, currentUser, true);
    }

    @Transactional(readOnly = true)
    public List<TeamProgressResponse> getMyProgress(User currentUser) {
        List<TeamMember> memberships = teamMemberRepository.findActiveTeamsByUserId(currentUser.getUserId());
        List<TeamProgressResponse> result = new ArrayList<>();
        for (TeamMember membership : memberships) {
            result.add(buildTeamProgress(membership.getTeam(), currentUser, true));
        }
        result.sort(Comparator.comparing(TeamProgressResponse::getProjectTitle,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectProgressResponse> getLecturerProgress(User currentUser) {
        if (currentUser.getRole() != Role.LECTURER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only lecturers can view this progress overview");
        }
        List<Project> projects = currentUser.getRole() == Role.ADMIN
                ? projectRepository.findAll()
                : projectRepository.findByLecturer_UserId(currentUser.getUserId());
        List<ProjectProgressResponse> result = new ArrayList<>();
        for (Project project : projects) {
            result.add(buildProjectProgress(project, currentUser, true));
        }
        result.sort(Comparator.comparing(ProjectProgressResponse::getProjectTitle,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    private ProjectProgressResponse buildProjectProgress(Project project, User currentUser, boolean includeMembers) {
        List<Team> teams = teamRepository.findByProject_ProjectId(project.getProjectId());
        ProjectProgressResponse response = new ProjectProgressResponse();
        response.setProjectId(project.getProjectId());
        response.setProjectTitle(project.getTitle());

        long taskTotal = 0;
        long taskCompleted = 0;
        long taskOverdue = 0;
        long milestoneTotal = 0;
        long milestoneCompleted = 0;
        int overdueTeams = 0;
        List<TeamProgressResponse> teamResponses = new ArrayList<>();

        for (Team team : teams) {
            TeamProgressResponse teamProgress = buildTeamProgress(team, currentUser, includeMembers);
            teamResponses.add(teamProgress);
            taskTotal += teamProgress.getTaskTotal();
            taskCompleted += teamProgress.getTaskCompleted();
            taskOverdue += teamProgress.getTaskOverdue();
            milestoneTotal += teamProgress.getMilestoneTotal();
            milestoneCompleted += teamProgress.getMilestoneCompleted();
            if (teamProgress.getTaskOverdue() > 0) {
                overdueTeams += 1;
            }
        }

        // Project-level tasks with no team still count toward project progress.
        List<Task> projectTasks = taskRepository.findByProjectIdWithAssignee(project.getProjectId());
        for (Task task : projectTasks) {
            if (task.getTeam() != null) {
                continue;
            }
            taskTotal += 1;
            if (task.getStatus() == TaskStatus.COMPLETED) {
                taskCompleted += 1;
            } else if (isOverdue(task)) {
                taskOverdue += 1;
            }
        }

        response.setTaskTotal(taskTotal);
        response.setTaskCompleted(taskCompleted);
        response.setTaskPercent(percent(taskCompleted, taskTotal));
        response.setTaskOverdue(taskOverdue);
        response.setMilestoneTotal(milestoneTotal);
        response.setMilestoneCompleted(milestoneCompleted);
        response.setMilestonePercent(percent(milestoneCompleted, milestoneTotal));
        response.setTeamCount(teams.size());
        response.setOverdueTeamCount(overdueTeams);
        teamResponses.sort(Comparator.comparing(TeamProgressResponse::getTeamName,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        response.setTeams(teamResponses);
        return response;
    }

    private TeamProgressResponse buildTeamProgress(Team team, User currentUser, boolean includeMembers) {
        TeamProgressResponse response = new TeamProgressResponse();
        response.setTeamId(team.getTeamId());
        response.setTeamName(team.getName());
        if (team.getProject() != null) {
            response.setProjectId(team.getProject().getProjectId());
            response.setProjectTitle(team.getProject().getTitle());
        }

        List<Task> tasks = taskRepository.findByTeamIdWithAssignee(team.getTeamId());
        if (tasks == null) {
            tasks = List.of();
        }
        long taskTotal = tasks.size();
        long taskCompleted = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long taskOverdue = tasks.stream().filter(this::isOverdue).count();
        response.setTaskTotal(taskTotal);
        response.setTaskCompleted(taskCompleted);
        response.setTaskPercent(percent(taskCompleted, taskTotal));
        response.setTaskOverdue(taskOverdue);

        List<Milestone> milestones = milestoneRepository.findByTeam_TeamId(team.getTeamId());
        if (milestones == null) {
            milestones = List.of();
        }
        long milestoneTotal = milestones.size();
        long milestoneCompleted = milestones.stream().filter(m -> Boolean.TRUE.equals(m.getIsCompleted())).count();
        response.setMilestoneTotal(milestoneTotal);
        response.setMilestoneCompleted(milestoneCompleted);
        response.setMilestonePercent(percent(milestoneCompleted, milestoneTotal));

        List<TeamMember> members = teamMemberRepository.findActiveMembersByTeamId(team.getTeamId());
        if (members == null) {
            members = List.of();
        }

        Map<Long, long[]> taskStats = new HashMap<>();
        for (TeamMember member : members) {
            taskStats.put(member.getUser().getUserId(), new long[]{0, 0, 0});
        }
        LocalDateTime now = LocalDateTime.now();
        for (Task task : tasks) {
            if (task.getAssignedTo() == null) {
                continue;
            }
            Long userId = task.getAssignedTo().getUserId();
            long[] stats = taskStats.computeIfAbsent(userId, id -> new long[]{0, 0, 0});
            stats[0] += 1;
            if (task.getStatus() == TaskStatus.COMPLETED) {
                stats[1] += 1;
            } else if (task.getDeadline() != null && task.getDeadline().isBefore(now)) {
                stats[2] += 1;
            }
        }

        Map<Long, Long> comments = new HashMap<>();
        addCounts(comments, commentRepository.countByUserForTeamTasks(team.getTeamId()));
        if (team.getProject() != null) {
            addCounts(comments, commentRepository.countByUserForProjectComments(team.getProject().getProjectId()));
        }

        Map<Long, Long> files = new HashMap<>();
        addCounts(files, fileRepository.countByUploaderForTeam(team.getTeamId()));
        addCounts(files, fileRepository.countByUploaderForTeamTaskComments(team.getTeamId()));

        Map<Long, Long> discussions = new HashMap<>();
        if (team.getProject() != null) {
            Long projectId = team.getProject().getProjectId();
            addCounts(discussions, discussionRepository.countByCreatorForProject(projectId));
            addCounts(discussions, discussionReplyRepository.countByUserForProject(projectId));
        }

        List<MemberContributionResponse> rows = new ArrayList<>();
        for (TeamMember member : members) {
            User user = member.getUser();
            long[] stats = taskStats.getOrDefault(user.getUserId(), new long[]{0, 0, 0});
            MemberContributionResponse row = new MemberContributionResponse();
            row.setUserId(user.getUserId());
            row.setName(displayName(user));
            row.setAssigned((int) stats[0]);
            row.setCompleted((int) stats[1]);
            row.setOverdue((int) stats[2]);
            row.setComments(comments.getOrDefault(user.getUserId(), 0L));
            row.setFiles(files.getOrDefault(user.getUserId(), 0L));
            row.setDiscussions(discussions.getOrDefault(user.getUserId(), 0L));
            rows.add(row);
            if (currentUser != null && user.getUserId().equals(currentUser.getUserId())) {
                response.setMyAssigned(row.getAssigned());
                response.setMyCompleted(row.getCompleted());
            }
        }
        rows.sort(Comparator
                .comparingInt(MemberContributionResponse::getCompleted).reversed()
                .thenComparing(MemberContributionResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)));

        if (includeMembers) {
            response.setMembers(rows);
        }
        return response;
    }

    private boolean isOverdue(Task task) {
        return DeadlineRules.isTaskOverdue(task, LocalDateTime.now());
    }

    private int percent(long completed, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((completed * 100.0) / total);
    }

    private void addCounts(Map<Long, Long> target, List<Object[]> rows) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long userId = ((Number) row[0]).longValue();
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            target.merge(userId, count, Long::sum);
        }
    }

    private String displayName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
