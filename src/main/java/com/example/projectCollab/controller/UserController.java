package com.example.projectCollab.controller;

import com.example.projectCollab.dto.AvailableUserResponse;
import com.example.projectCollab.dto.UserResponse;
import com.example.projectCollab.dto.UserUpdateRequest;
import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.TeamMember;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.entity.UserStatus;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.UserRepository;
import com.example.projectCollab.service.UserService;
import com.example.projectCollab.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthUtil authUtil;
    private final TeamMemberRepository teamMemberRepository;

    public UserController(UserRepository userRepository,
                          UserService userService,
                          AuthUtil authUtil,
                          TeamMemberRepository teamMemberRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.authUtil = authUtil;
        this.teamMemberRepository = teamMemberRepository;
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
    // ✅ NEW: GET AVAILABLE USERS FOR INVITATION
    // ==========================================

    @GetMapping("/available")
    public ResponseEntity<List<AvailableUserResponse>> getAvailableUsers() {
        // Get all users with role STUDENT or TEAM_LEADER and status ACTIVE
        List<User> users = userRepository.findByRoleInAndStatus(
                List.of(Role.STUDENT, Role.TEAM_LEADER),
                UserStatus.ACTIVE
        );

        List<AvailableUserResponse> responses = users.stream()
                .map(user -> {
                    // Get current active teams for this user
                    List<TeamMember> teamMembers = teamMemberRepository.findActiveTeamsByUserId(user.getUserId());

                    List<AvailableUserResponse.TeamInfo> currentTeams = teamMembers.stream()
                            .map(tm -> {
                                boolean isTeamLeader = tm.getTeam().getTeamLeader() != null &&
                                        tm.getTeam().getTeamLeader().getUserId().equals(user.getUserId());
                                return new AvailableUserResponse.TeamInfo(
                                        tm.getTeam().getTeamId(),
                                        tm.getTeam().getName(),
                                        isTeamLeader ? "TEAM_LEADER" : "MEMBER"
                                );
                            })
                            .collect(Collectors.toList());

                    return new AvailableUserResponse(
                            user.getUserId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name(),
                            user.getStatus().name(),
                            currentTeams
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
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