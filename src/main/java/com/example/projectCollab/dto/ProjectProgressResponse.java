package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectProgressResponse {
    private Long projectId;
    private String projectTitle;

    private long taskTotal;
    private long taskCompleted;
    private int taskPercent;
    private long taskOverdue;

    private long milestoneTotal;
    private long milestoneCompleted;
    private int milestonePercent;

    private int teamCount;
    private int overdueTeamCount;

    private List<TeamProgressResponse> teams = new ArrayList<>();
}
