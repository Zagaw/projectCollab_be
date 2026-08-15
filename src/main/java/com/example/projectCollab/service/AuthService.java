package com.example.projectCollab.service;

import com.example.projectCollab.dto.AuthResponse;
import com.example.projectCollab.dto.LoginRequest;
import com.example.projectCollab.dto.RegisterRequest;
import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.entity.UserStatus;
import com.example.projectCollab.exception.EmailAlreadyExistsException;
import com.example.projectCollab.repository.UserRepository;
import com.example.projectCollab.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // ==========================================
    // REGISTER
    // ==========================================

    public AuthResponse register(
            RegisterRequest request
    ) {

        // Check email
        if (userRepository.existsByEmail(
                request.email()
        )) {

            throw new EmailAlreadyExistsException(
                    "Email is already registered"
            );
        }

        // Check username
        if (userRepository.existsByUsername(
                request.username()
        )) {

            throw new IllegalArgumentException(
                    "Username is already taken"
            );
        }

        // Check student ID if provided
        if (request.studentId() != null &&
                !request.studentId().isBlank() &&
                userRepository.existsByStudentId(
                        request.studentId()
                )) {

            throw new IllegalArgumentException(
                    "Student ID is already registered"
            );
        }

        User user = new User();

        user.setUsername(
                request.username().trim()
        );

        user.setEmail(
                request.email().trim().toLowerCase()
        );

        user.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setFirstName(
                request.firstName().trim()
        );

        user.setLastName(
                request.lastName().trim()
        );

        user.setStudentId(
                request.studentId()
        );

        user.setPhone(
                request.phone()
        );

        // IMPORTANT:
        // Never allow public registration
        // to choose ADMIN or LECTURER.
        /*user.setRole(Role.STUDENT);

        user.setStatus(UserStatus.ACTIVE);

        User savedUser =
                userRepository.save(user);
        */

        // ==========================================
        // ROLE HANDLING WITH VERIFICATION
        // ==========================================

        Role role;
        UserStatus status;

        // Check if role is provided and valid
        String requestedRole = request.role();

        if (requestedRole != null && !requestedRole.isBlank()) {
            try {
                role = Role.valueOf(requestedRole.toUpperCase());

                // Security: Only allow STUDENT or LECTURER registration
                // ADMIN and TEAM_LEADER must be assigned by admin
                if (role == Role.ADMIN) {
                    throw new IllegalArgumentException("Cannot register as ADMIN. Contact system administrator.");
                }

                if (role == Role.LECTURER) {
                    // Lecturers need admin verification
                    status = UserStatus.PENDING_VERIFICATION;
                } else {
                    // Students are active immediately
                    status = UserStatus.ACTIVE;
                }
            } catch (IllegalArgumentException e) {
                // Invalid role, default to STUDENT
                role = Role.STUDENT;
                status = UserStatus.ACTIVE;
            }
        } else {
            // Default to STUDENT if no role provided
            role = Role.STUDENT;
            status = UserStatus.ACTIVE;
        }

        user.setRole(role);
        user.setStatus(status);

        User savedUser = userRepository.save(user);

        // Create UserDetails
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .authorities(
                                "ROLE_" +
                                        savedUser.getRole().name()
                        )
                        .build();

        // Generate JWT
        String token =
                jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getStatus().name()  // Add this 7th parameter
        );
    }


    // ==========================================
    // LOGIN
    // ==========================================

    public AuthResponse login(
            LoginRequest request
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(),
                                request.password()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user =
                userRepository.findByEmail(
                                request.email()
                                        .trim()
                                        .toLowerCase()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        String token =
                jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
