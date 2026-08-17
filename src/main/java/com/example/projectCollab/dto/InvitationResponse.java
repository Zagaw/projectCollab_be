package com.example.projectCollab.dto;

import com.example.projectCollab.entity.TeamMember;
import com.example.projectCollab.entity.TeamMemberStatus;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long invitationId,
        Long teamId,
        String teamName,
        Long projectId,
        String projectTitle,
        String inviterName,
        String inviterEmail,
        String status,
        LocalDateTime invitedAt,
        String message
) {
    public static InvitationResponse fromEntity(TeamMember teamMember) {
        // FIX: Handle null team leader
        String inviterName = "System";
        String inviterEmail = "system@collabora.com";

        if (teamMember.getTeam().getTeamLeader() != null) {
            inviterName = teamMember.getTeam().getTeamLeader().getFirstName() + " " +
                    teamMember.getTeam().getTeamLeader().getLastName();
            inviterEmail = teamMember.getTeam().getTeamLeader().getEmail();
        }

        // FIX: Handle null project
        String projectTitle = "Unknown Project";
        if (teamMember.getTeam().getProject() != null) {
            projectTitle = teamMember.getTeam().getProject().getTitle();
        }

        return new InvitationResponse(
                teamMember.getTeamMemberId(),
                teamMember.getTeam().getTeamId(),
                teamMember.getTeam().getName(),
                teamMember.getTeam().getProject().getProjectId(),
                projectTitle,
                inviterName,
                inviterEmail,
                teamMember.getStatus().name(),
                teamMember.getInvitedAt(),
                "You have been invited to join team '" + teamMember.getTeam().getName() +
                        "' for project '" + projectTitle + "'"
        );
    }
}