package com.example.projectCollab.service;

import com.example.projectCollab.dto.*;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import com.example.projectCollab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamService(TeamRepository teamRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    // ==========================================
    // CREATE TEAM
    // ==========================================

    @Transactional
    public TeamResponse createTeam(TeamRequest request, Long lecturerId) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify lecturer owns this project
        if (!project.getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to create teams for this project");
        }

        // Check if project is active
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("Cannot create team for non-active project");
        }

        Team team = new Team();
        team.setName(request.name());
        team.setDescription(request.description());
        team.setProject(project);

        Team savedTeam = teamRepository.save(team);
        return TeamResponse.fromEntity(savedTeam);
    }

    // ==========================================
    // GET TEAM BY ID
    // ==========================================

    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Force loading of members using the correct method
        List<TeamMember> members = teamMemberRepository.findMembersByTeamId(teamId);
        team.setMembers(members);

        return TeamResponse.fromEntity(team);
    }

    // ==========================================
    // GET TEAMS BY PROJECT
    // ==========================================

    public List<TeamResponse> getTeamsByProject(Long projectId) {
        List<Team> teams = teamRepository.findByProject_ProjectId(projectId);
        return teams.stream()
                .map(TeamResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET TEAMS BY MEMBER
    // ==========================================

    public List<TeamResponse> getTeamsByMember(Long userId) {
        List<Team> teams = teamRepository.findTeamsByMemberId(userId);
        return teams.stream()
                .map(TeamResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // UPDATE TEAM
    // ==========================================

    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamRequest request, Long lecturerId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify lecturer owns the project
        if (!team.getProject().getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to update this team");
        }

        team.setName(request.name());
        team.setDescription(request.description());

        Team updatedTeam = teamRepository.save(team);
        return TeamResponse.fromEntity(updatedTeam);
    }

    // ==========================================
    // DELETE TEAM
    // ==========================================

    @Transactional
    public void deleteTeam(Long teamId, Long lecturerId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify lecturer owns the project
        if (!team.getProject().getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to delete this team");
        }

        teamRepository.delete(team);
    }

    // ==========================================
    // ASSIGN TEAM LEADER
    // ==========================================

    @Transactional
    public TeamResponse assignTeamLeader(Long teamId, Long userId, Long lecturerId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify lecturer owns the project
        if (!team.getProject().getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to assign team leader");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is a member of the team
        TeamMember member = teamMemberRepository
                .findByTeam_TeamIdAndUser_UserId(teamId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this team"));

        // Check if member is active
        if (member.getStatus() != TeamMemberStatus.ACTIVE) {
            throw new IllegalStateException("User must be an active member to be team leader");
        }

        // If there was a previous team leader, update their role back to STUDENT
        if (team.getTeamLeader() != null) {
            User previousLeader = team.getTeamLeader();
            // You might want to log this or send notification
        }

        team.setTeamLeader(user);
        Team updatedTeam = teamRepository.save(team);

        // Update user's role to TEAM_LEADER if they aren't already
        if (user.getRole() != Role.TEAM_LEADER && user.getRole() != Role.ADMIN) {
            user.setRole(Role.TEAM_LEADER);
            userRepository.save(user);
        }

        return TeamResponse.fromEntity(updatedTeam);
    }

    // Add this method to TeamService for better data loading
    @Transactional(readOnly = true)
    public TeamResponse getTeamByIdWithMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        // Force loading of members
        team.getMembers().size();
        return TeamResponse.fromEntity(team);
    }
}