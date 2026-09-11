package com.example.projectCollab.insights;

import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.TeamRiskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmRiskSummaryGenerator {

    private static final Logger log = LoggerFactory.getLogger(LlmRiskSummaryGenerator.class);
    private static final Pattern JSON_STRING = Pattern.compile("\"(?:text|content)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final RestClient restClient;
    private final TemplateRiskSummaryGenerator templateGenerator;

    @Value("${collabora.insights.llm.enabled:false}")
    private boolean enabled;

    @Value("${collabora.insights.llm.provider:gemini}")
    private String provider;

    @Value("${collabora.insights.llm.api-key:}")
    private String apiKey;

    @Value("${collabora.insights.llm.model:gemini-2.0-flash}")
    private String model;

    public LlmRiskSummaryGenerator(TemplateRiskSummaryGenerator templateGenerator) {
        this.restClient = RestClient.create();
        this.templateGenerator = templateGenerator;
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public RiskSummaryResponse generate(TeamRiskResponse risk) {
        if (!isEnabled()) {
            return templateGenerator.generate(risk);
        }
        try {
            String text = "gemini".equalsIgnoreCase(provider) ? callGemini(risk) : callOpenAi(risk);
            if (text == null || text.isBlank()) {
                return templateGenerator.generate(risk);
            }
            return new RiskSummaryResponse(text.trim(), "LLM");
        } catch (Exception ex) {
            log.warn("LLM risk summary failed, using template: {}", ex.getMessage());
            return templateGenerator.generate(risk);
        }
    }

    private String prompt(TeamRiskResponse risk) {
        String teamName = risk.getTeamName() == null ? "This team" : risk.getTeamName();
        String projectTitle = risk.getProjectTitle() == null ? "a project" : risk.getProjectTitle();
        return "Write 3 to 5 short sentences for a university lecturer about this student project team. "
                + "Use only the facts given. Do not invent work, people, or deadlines. "
                + "End with one recommended action.\n"
                + "Team: " + teamName + "\n"
                + "Project: " + projectTitle + "\n"
                + "Risk band: " + TemplateRiskSummaryGenerator.bandLabel(risk.getBand()) + "\n"
                + "Score: " + risk.getScore() + " / 100\n"
                + "Overdue tasks: " + risk.getOverdueTaskCount() + "\n"
                + "Tasks due within 48 hours: " + risk.getDueSoonTaskCount() + "\n"
                + "Overdue milestones: " + risk.getOverdueMilestoneCount() + "\n"
                + "Milestones due within 48 hours: " + risk.getDueSoonMilestoneCount() + "\n"
                + "Completed tasks: " + risk.getTaskCompleted() + " of " + risk.getTaskTotal() + "\n"
                + "Reasons: " + TemplateRiskSummaryGenerator.joinReasons(risk.getReasons());
    }

    private String callGemini(TeamRiskResponse risk) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt(risk)))
                ))
        );
        String json = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return firstJsonString(json, "text");
    }

    private String callOpenAi(TeamRiskResponse risk) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null || model.isBlank() ? "gpt-4o-mini" : model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You summarize project deadline risk for lecturers. Use only provided facts."),
                Map.of("role", "user", "content", prompt(risk))
        ));
        String json = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        return firstJsonString(json, "content");
    }

    private String firstJsonString(String json, String preferredKey) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Pattern preferred = Pattern.compile("\"" + Pattern.quote(preferredKey) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = preferred.matcher(json);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        matcher = JSON_STRING.matcher(json);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return null;
    }

    private String unescape(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
