package com.example.projectCollab.dto;

import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.ProjectStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record ProjectResponse(
        Long projectId,
        String title,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String course,
        String semester,
        String lecturerName,
        String lecturerEmail,
        ProjectStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer teamCount,
        List<TeamSummary> teams
) {
    public record TeamSummary(
            Long teamId,
            String teamName,
            Integer memberCount,
            String teamLeaderName
    ) {}

    public static ProjectResponse fromEntity(Project project) {
        // FIX: Ensure lecturer is loaded
        String lecturerName = "Unknown";
        String lecturerEmail = "Unknown";

        if (project.getLecturer() != null) {
            lecturerName = project.getLecturer().getFirstName() + " " +
                    project.getLecturer().getLastName();
            lecturerEmail = project.getLecturer().getEmail();
        }

        List<TeamSummary> teamSummaries = project.getTeams().stream()
                .map(team -> {
                    String leaderName = "Not assigned";
                    if (team.getTeamLeader() != null) {
                        leaderName = team.getTeamLeader().getFirstName() + " " +
                                team.getTeamLeader().getLastName();
                    }
                    return new TeamSummary(
                            team.getTeamId(),
                            team.getName(),
                            team.getMembers().size(),
                            leaderName
                    );
                })
                .collect(Collectors.toList());

        return new ProjectResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCourse(),
                project.getSemester(),
                lecturerName,
                lecturerEmail,
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getTeams().size(),
                teamSummaries
        );
    }
}