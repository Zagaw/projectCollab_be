package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String type;
    private String title;
    private LocalDateTime generatedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long projectId;
    private String projectTitle;
    private Long teamId;
    private String teamName;
    private List<ReportMetric> metrics = new ArrayList<>();
    private List<ReportSection> sections = new ArrayList<>();
}
