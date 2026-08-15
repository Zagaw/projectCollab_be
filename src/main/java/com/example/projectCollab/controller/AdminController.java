package com.example.projectCollab.controller;

import com.example.projectCollab.dto.UserResponse;
import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.entity.UserStatus;
import com.example.projectCollab.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ==========================================
    // GET PENDING LECTURERS
    // ==========================================

    @GetMapping("/pending-lecturers")
    public ResponseEntity<List<UserResponse>> getPendingLecturers() {
        List<User> pendingLecturers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.LECTURER
                        && user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                pendingLecturers.stream()
                        .map(UserResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    // ==========================================
    // VERIFY LECTURER
    // ==========================================

    @PutMapping("/verify-lecturer/{userId}")
    public ResponseEntity<?> verifyLecturer(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is not a lecturer"));
        }

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is not pending verification"));
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Lecturer verified successfully",
                "user", UserResponse.fromEntity(user)
        ));
    }

    // ==========================================
    // REJECT LECTURER
    // ==========================================

    @PutMapping("/reject-lecturer/{userId}")
    public ResponseEntity<?> rejectLecturer(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is not a lecturer"));
        }

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is not pending verification"));
        }

        // Option 1: Delete the user
        userRepository.delete(user);

        // Option 2: Set as INACTIVE (commented out)
        // user.setStatus(UserStatus.INACTIVE);
        // userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Lecturer registration rejected and removed"
        ));
    }

    // ==========================================
    // GET ALL USERS (Admin Only)
    // ==========================================

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(
                users.stream()
                        .map(UserResponse::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    // ==========================================
    // UPDATE USER ROLE (Admin Only)
    // ==========================================

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newRole = request.get("role");
        if (newRole == null || newRole.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Role is required"));
        }

        try {
            Role role = Role.valueOf(newRole.toUpperCase());
            user.setRole(role);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "User role updated successfully",
                    "user", UserResponse.fromEntity(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid role: " + newRole));
        }
    }

    // ==========================================
    // UPDATE USER STATUS (Admin Only)
    // ==========================================

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newStatus = request.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Status is required"));
        }

        try {
            UserStatus status = UserStatus.valueOf(newStatus.toUpperCase());
            user.setStatus(status);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "User status updated successfully",
                    "user", UserResponse.fromEntity(user)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status: " + newStatus));
        }
    }
}