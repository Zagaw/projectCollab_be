package com.example.projectCollab.controller;

import com.example.projectCollab.dto.UserResponse;
import com.example.projectCollab.dto.UserUpdateRequest;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.repository.UserRepository;
import com.example.projectCollab.service.UserService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthUtil authUtil;

    public UserController(UserRepository userRepository,
                          UserService userService,
                          AuthUtil authUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // GET CURRENT USER PROFILE
    // ==========================================

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = authUtil.getCurrentUser(authentication);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    // ==========================================
    // UPDATE CURRENT USER PROFILE
    // ==========================================

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        UserResponse updatedUser = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    // ==========================================
    // UPDATE PASSWORD ONLY
    // ==========================================

    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Long userId = authUtil.getCurrentUserId(authentication);
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        userService.updatePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // ==========================================
    // GET USER BY ID (Admin only)
    // ==========================================

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}