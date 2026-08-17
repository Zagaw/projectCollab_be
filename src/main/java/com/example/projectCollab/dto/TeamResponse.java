package com.example.projectCollab.dto;

import com.example.projectCollab.entity.Team;
import com.example.projectCollab.entity.TeamMember;
import com.example.projectCollab.entity.TeamMemberStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record TeamResponse(
        Long teamId,
        String name,
        String description,
        Long projectId,
        String projectTitle,
        TeamLeaderInfo teamLeader,
        List<MemberInfo> members,
        Integer totalMembers,
        Integer activeMembers,
        Integer pendingMembers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record TeamLeaderInfo(
            Long userId,
            String username,
            String email,
            String fullName
    ) {}

    public record MemberInfo(
            Long teamMemberId,
            Long userId,
            String username,
            String email,
            String fullName,
            String status,
            LocalDateTime joinedAt,
            LocalDateTime invitedAt
    ) {}

    public static TeamResponse fromEntity(Team team) {
        TeamLeaderInfo leaderInfo = team.getTeamLeader() != null ?
                new TeamLeaderInfo(
                        team.getTeamLeader().getUserId(),
                        team.getTeamLeader().getUsername(),
                        team.getTeamLeader().getEmail(),
                        team.getTeamLeader().getFirstName() + " " + team.getTeamLeader().getLastName()
                ) : null;

        List<MemberInfo> memberInfos = team.getMembers().stream()
                .map(member -> new MemberInfo(
                        member.getTeamMemberId(),
                        member.getUser().getUserId(),
                        member.getUser().getUsername(),
                        member.getUser().getEmail(),
                        member.getUser().getFirstName() + " " + member.getUser().getLastName(),
                        member.getStatus().name(),
                        member.getJoinedAt(),
                        member.getInvitedAt()
                ))
                .collect(Collectors.toList());

        long activeCount = team.getMembers().stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACTIVE)
                .count();

        long pendingCount = team.getMembers().stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.PENDING)
                .count();

        return new TeamResponse(
                team.getTeamId(),
                team.getName(),
                team.getDescription(),
                team.getProject().getProjectId(),
                team.getProject().getTitle(),
                leaderInfo,
                memberInfos,
                team.getMembers().size(),
                (int) activeCount,
                (int) pendingCount,
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
