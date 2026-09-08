package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableUserResponse {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private List<TeamInfo> currentTeams;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamInfo {
        private Long teamId;
        private String teamName;
        private String role; // "TEAM_LEADER" or "MEMBER"
    }

    // Helper method to check if user is in any team
    public boolean isInAnyTeam() {
        return currentTeams != null && !currentTeams.isEmpty();
    }

    // Helper method to get team names as string
    public String getTeamNames() {
        if (currentTeams == null || currentTeams.isEmpty()) {
            return "Not in any team";
        }
        return currentTeams.stream()
                .map(TeamInfo::getTeamName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    // Helper method to get full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}