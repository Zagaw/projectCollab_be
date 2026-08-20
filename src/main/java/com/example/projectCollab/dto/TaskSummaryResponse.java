package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskSummaryResponse {
    private long totalTasks;
    private long todoCount;
    private long inProgressCount;
    private long completedCount;
    private double completionPercentage;
}