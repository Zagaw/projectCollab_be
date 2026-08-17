package com.example.projectCollab.service;

import com.example.projectCollab.dto.InvitationResponse;
import com.example.projectCollab.dto.TeamMemberRequest;
import com.example.projectCollab.dto.TeamResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import com.example.projectCollab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamMemberService(TeamMemberRepository teamMemberRepository,
                             TeamRepository teamRepository,
                             UserRepository userRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // INVITE STUDENT TO TEAM
    // ==========================================

    @Transactional
    public InvitationResponse inviteStudent(TeamMemberRequest request, Long lecturerId) {
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify lecturer owns the project
        if (!team.getProject().getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to invite students to this team");
        }

        User student = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if student is already in the team
        if (teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(request.teamId(), request.userId())) {
            throw new IllegalStateException("Student is already a member of this team");
        }

        // Check if student is a student role
        if (student.getRole() != Role.STUDENT && student.getRole() != Role.TEAM_LEADER) {
            throw new IllegalArgumentException("Only students can be invited to teams");
        }

        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(student);
        teamMember.setStatus(TeamMemberStatus.PENDING);

        TeamMember saved = teamMemberRepository.save(teamMember);

        return InvitationResponse.fromEntity(saved);
    }

    // ==========================================
    // ACCEPT INVITATION
    // ==========================================

    @Transactional
    public TeamResponse acceptInvitation(Long invitationId, Long userId) {
        TeamMember teamMember = teamMemberRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify the invitation belongs to this user
        if (!teamMember.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("This invitation does not belong to you");
        }

        if (teamMember.getStatus() != TeamMemberStatus.PENDING) {
            throw new IllegalStateException("This invitation is no longer pending");
        }

        teamMember.setStatus(TeamMemberStatus.ACTIVE);
        teamMember.setJoinedAt(LocalDateTime.now());

        TeamMember updated = teamMemberRepository.save(teamMember);
        return TeamResponse.fromEntity(updated.getTeam());
    }

    // ==========================================
    // REJECT INVITATION
    // ==========================================

    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        TeamMember teamMember = teamMemberRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify the invitation belongs to this user
        if (!teamMember.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("This invitation does not belong to you");
        }

        if (teamMember.getStatus() != TeamMemberStatus.PENDING) {
            throw new IllegalStateException("This invitation is no longer pending");
        }

        teamMember.setStatus(TeamMemberStatus.REJECTED);
        teamMemberRepository.save(teamMember);
    }

    // ==========================================
    // GET USER'S INVITATIONS
    // ==========================================

    public List<InvitationResponse> getMyInvitations(Long userId) {
        // Use the correct repository method
        List<TeamMember> pendingInvitations = teamMemberRepository
                .findByUser_UserIdAndStatus(userId, TeamMemberStatus.PENDING);

        if (pendingInvitations.isEmpty()) {
            return List.of();
        }

        return pendingInvitations.stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET TEAM MEMBERS
    // ==========================================

    public List<TeamMember> getTeamMembers(Long teamId) {
        // Use the correct method name
        return teamMemberRepository.findMembersByTeamId(teamId);
    }

    // ==========================================
    // GET ACTIVE TEAM MEMBERS
    // ==========================================

    public List<TeamMember> getActiveTeamMembers(Long teamId) {
        return teamMemberRepository.findActiveMembersByTeamId(teamId);
    }

    // ==========================================
    // REMOVE MEMBER FROM TEAM
    // ==========================================

    @Transactional
    public void removeMember(Long teamId, Long memberId, Long lecturerId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Verify lecturer owns the project
        if (!team.getProject().getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to remove members from this team");
        }

        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Cannot remove team leader
        if (team.getTeamLeader() != null &&
                team.getTeamLeader().getUserId().equals(member.getUser().getUserId())) {
            throw new IllegalStateException("Cannot remove the team leader. Assign a new leader first.");
        }

        member.setStatus(TeamMemberStatus.REMOVED);
        teamMemberRepository.save(member);
    }

    // ==========================================
    // GET MY ACTIVE TEAMS
    // ==========================================

    public List<TeamResponse> getMyActiveTeams(Long userId) {
        List<TeamMember> activeMemberships = teamMemberRepository
                .findByUser_UserIdAndStatus(userId, TeamMemberStatus.ACTIVE);

        return activeMemberships.stream()
                .map(TeamMember::getTeam)
                .map(TeamResponse::fromEntity)
                .collect(Collectors.toList());
    }
}