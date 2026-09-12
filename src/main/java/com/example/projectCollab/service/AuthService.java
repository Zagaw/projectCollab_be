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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

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
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String username = request.username() == null ? "" : request.username().trim();
        String studentId = blankToNull(request.studentId());
        String phone = blankToNull(request.phone());

        Role role = resolveRegisterRole(request.role());
        UserStatus status = role == Role.LECTURER ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE;

        if (role == Role.LECTURER) {
            studentId = null;
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (studentId != null && userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("Student ID is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStudentId(studentId);
        user.setPhone(phone);
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
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().name(),
                savedUser.getStatus().name()
        );
    }


    // ==========================================
    // LOGIN
    // ==========================================

    public AuthResponse login(
            LoginRequest request
    ) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalStateException("This account is inactive. Contact an administrator.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("This account has been suspended. Contact an administrator.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException ex) {
            throw new IllegalStateException("This account is not active. Contact an administrator.");
        } catch (LockedException ex) {
            throw new IllegalStateException("This account has been suspended. Contact an administrator.");
        }

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities("ROLE_" + user.getRole().name())
                        .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    private Role resolveRegisterRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.STUDENT;
        }
        Role role;
        try {
            role = Role.valueOf(requestedRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Role.STUDENT;
        }
        if (role == Role.ADMIN || role == Role.TEAM_LEADER) {
            throw new IllegalArgumentException("That role cannot be chosen during sign up.");
        }
        return role;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
