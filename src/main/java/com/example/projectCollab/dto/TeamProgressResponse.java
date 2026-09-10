package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamProgressResponse {
    private Long teamId;
    private String teamName;
    private Long projectId;
    private String projectTitle;

    private long taskTotal;
    private long taskCompleted;
    private int taskPercent;
    private long taskOverdue;

    private long milestoneTotal;
    private long milestoneCompleted;
    private int milestonePercent;

    private Integer myAssigned;
    private Integer myCompleted;

    private List<MemberContributionResponse> members = new ArrayList<>();
}
