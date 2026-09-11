package com.example.projectCollab.insights;

import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.TeamRiskResponse;

public interface RiskSummaryGenerator {
    RiskSummaryResponse generate(TeamRiskResponse risk);
}
