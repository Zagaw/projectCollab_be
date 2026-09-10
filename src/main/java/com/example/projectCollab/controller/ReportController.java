package com.example.projectCollab.controller;

import com.example.projectCollab.dto.ReportResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.ReportService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AuthUtil authUtil;

    @GetMapping("/api/reports")
    public ResponseEntity<ReportResponse> getReport(
            @RequestParam String type,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime to,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(reportService.generate(type, projectId, teamId, from, to, currentUser));
    }
}
