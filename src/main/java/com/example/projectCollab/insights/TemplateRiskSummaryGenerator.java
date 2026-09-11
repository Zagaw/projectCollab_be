package com.example.projectCollab.insights;

import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.TeamRiskResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TemplateRiskSummaryGenerator implements RiskSummaryGenerator {

    @Override
    public RiskSummaryResponse generate(TeamRiskResponse risk) {
        String teamName = risk.getTeamName() == null ? "This team" : risk.getTeamName();
        String band = bandLabel(risk.getBand());
        String reasons = joinReasons(risk.getReasons());
        String summary = teamName + " is " + band + " (" + risk.getScore() + "). " + reasons
                + " Recommended action: focus on overdue work first, then items due within 48 hours.";
        return new RiskSummaryResponse(summary, "TEMPLATE");
    }

    static String bandLabel(String band) {
        if (TeamRiskAssessment.AT_RISK.equals(band)) {
            return "At risk";
        }
        if (TeamRiskAssessment.WATCH.equals(band)) {
            return "Watch";
        }
        return "Healthy";
    }

    static String joinReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "No deadline risks right now.";
        }
        return reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.joining(". ")) + ".";
    }
}
