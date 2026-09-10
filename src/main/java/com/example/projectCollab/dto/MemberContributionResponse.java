package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberContributionResponse {
    private Long userId;
    private String name;
    private int assigned;
    private int completed;
    private int overdue;
    private long comments;
    private long files;
    private long discussions;
}
