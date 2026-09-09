package com.example.projectCollab.service;

import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public ProjectAccessService(ProjectRepository projectRepository,
                                TeamRepository teamRepository,
                                TeamMemberRepository teamMemberRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    public Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    public Team requireTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }

    public boolean hasAccessToProject(Long projectId, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }
        if (project.getLecturer() != null
                && project.getLecturer().getUserId().equals(user.getUserId())) {
            return true;
        }
        List<Team> teams = teamRepository.findByProject_ProjectId(projectId);
        for (Team team : teams) {
            if (teamMemberRepository.isActiveMember(team.getTeamId(), user.getUserId())) {
                return true;
            }
        }
        return false;
    }

    public void requireProjectAccess(Long projectId, User user) {
        if (!hasAccessToProject(projectId, user)) {
            throw new UnauthorizedAccessException("You don't have permission to access this project");
        }
    }

    public boolean hasAccessToTeam(Long teamId, User user) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return false;
        }
        if (team.getProject() != null
                && team.getProject().getLecturer() != null
                && team.getProject().getLecturer().getUserId().equals(user.getUserId())) {
            return true;
        }
        return teamMemberRepository.isActiveMember(teamId, user.getUserId());
    }

    public void requireTeamAccess(Long teamId, User user) {
        if (!hasAccessToTeam(teamId, user)) {
            throw new UnauthorizedAccessException("You don't have permission to access this team");
        }
    }

    public List<User> getProjectNotifyRecipients(Project project) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (project.getLecturer() != null) {
            recipients.put(project.getLecturer().getUserId(), project.getLecturer());
        }
        List<Team> teams = teamRepository.findByProject_ProjectId(project.getProjectId());
        for (Team team : teams) {
            addActiveMembers(recipients, team.getTeamId());
        }
        return new ArrayList<>(recipients.values());
    }

    public List<User> getTeamNotifyRecipients(Team team) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (team.getProject() != null && team.getProject().getLecturer() != null) {
            User lecturer = team.getProject().getLecturer();
            recipients.put(lecturer.getUserId(), lecturer);
        }
        addActiveMembers(recipients, team.getTeamId());
        return new ArrayList<>(recipients.values());
    }

    public List<User> getActiveTeamMembers(Long teamId) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        addActiveMembers(recipients, teamId);
        return new ArrayList<>(recipients.values());
    }

    public Team findUserTeamOnProject(Long projectId, Long userId) {
        List<TeamMember> memberships = teamMemberRepository.findActiveTeamsByUserId(userId);
        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            if (team.getProject() != null && team.getProject().getProjectId().equals(projectId)) {
                return team;
            }
        }
        return null;
    }

    public Team resolveLibraryUploadTeam(Project project, Long requestedTeamId, User user) {
        if (requestedTeamId != null) {
            Team team = requireTeam(requestedTeamId);
            if (team.getProject() == null || !team.getProject().getProjectId().equals(project.getProjectId())) {
                throw new IllegalArgumentException("Team does not belong to this project");
            }
            requireTeamAccess(team.getTeamId(), user);
            return team;
        }
        if (user.getRole() == Role.LECTURER || user.getRole() == Role.ADMIN) {
            return null;
        }
        Team team = findUserTeamOnProject(project.getProjectId(), user.getUserId());
        if (team == null) {
            throw new IllegalArgumentException("Select which team this file belongs to");
        }
        return team;
    }

    private void addActiveMembers(Map<Long, User> recipients, Long teamId) {
        List<TeamMember> members = teamMemberRepository.findActiveMembersByTeamId(teamId);
        for (TeamMember member : members) {
            User user = member.getUser();
            recipients.put(user.getUserId(), user);
        }
    }
}
