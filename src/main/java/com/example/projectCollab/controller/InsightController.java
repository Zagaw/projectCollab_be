package com.example.projectCollab.controller;

import com.example.projectCollab.dto.MyInsightResponse;
import com.example.projectCollab.dto.RiskSummaryResponse;
import com.example.projectCollab.dto.ScanResultResponse;
import com.example.projectCollab.dto.TeamRiskResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.insights.RiskReminderService;
import com.example.projectCollab.service.InsightService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;
    private final RiskReminderService riskReminderService;
    private final AuthUtil authUtil;

    @GetMapping("/me")
    public ResponseEntity<MyInsightResponse> getMyInsights(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(insightService.getMyInsights(currentUser));
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<List<TeamRiskResponse>> getLecturerInsights(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(insightService.getLecturerInsights(currentUser));
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<TeamRiskResponse> getTeamInsight(
            @PathVariable Long teamId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(insightService.getTeamInsight(teamId, currentUser));
    }

    @PostMapping("/teams/{teamId}/summary")
    public ResponseEntity<RiskSummaryResponse> summarizeTeam(
            @PathVariable Long teamId,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(insightService.summarizeTeam(teamId, currentUser));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    public ResponseEntity<ScanResultResponse> scanNow() {
        return ResponseEntity.ok(riskReminderService.scanNow());
    }
}
