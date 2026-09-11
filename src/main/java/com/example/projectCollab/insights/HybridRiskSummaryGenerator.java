package com.example.projectCollab.insights;

import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.TeamRiskResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class HybridRiskSummaryGenerator implements RiskSummaryGenerator {

    private final TemplateRiskSummaryGenerator templateGenerator;
    private final LlmRiskSummaryGenerator llmGenerator;

    public HybridRiskSummaryGenerator(TemplateRiskSummaryGenerator templateGenerator,
                                      LlmRiskSummaryGenerator llmGenerator) {
        this.templateGenerator = templateGenerator;
        this.llmGenerator = llmGenerator;
    }

    @Override
    public RiskSummaryResponse generate(TeamRiskResponse risk) {
        if (llmGenerator.isEnabled()) {
            return llmGenerator.generate(risk);
        }
        return templateGenerator.generate(risk);
    }
}
