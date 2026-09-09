package com.example.projectCollab.controller;

import com.example.projectCollab.dto.NotificationResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.NotificationService;
import com.example.projectCollab.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUser, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(currentUser)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.markRead(id, currentUser));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        User currentUser = authUtil.getCurrentUser(authentication);
        notificationService.markAllRead(currentUser);
        return ResponseEntity.noContent().build();
    }
}
