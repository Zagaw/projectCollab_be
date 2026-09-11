package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRiskResponse {
    private Long teamId;
    private String teamName;
    private Long projectId;
    private String projectTitle;
    private int score;
    private String band;
    private List<String> reasons = new ArrayList<>();
    private long overdueTaskCount;
    private long dueSoonTaskCount;
    private long overdueMilestoneCount;
    private long dueSoonMilestoneCount;
    private long taskTotal;
    private long taskCompleted;
    private List<DeadlineItemResponse> overdueItems = new ArrayList<>();
    private List<DeadlineItemResponse> dueSoonItems = new ArrayList<>();
}
