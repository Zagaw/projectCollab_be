package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyInsightResponse {
    private List<DeadlineItemResponse> myOverdue = new ArrayList<>();
    private List<DeadlineItemResponse> myDueSoon = new ArrayList<>();
    private List<TeamRiskResponse> teams = new ArrayList<>();
}
