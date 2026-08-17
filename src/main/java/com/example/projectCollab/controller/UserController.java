package com.example.projectCollab.controller;

import com.example.projectCollab.dto.UserResponse;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.repository.UserRepository;
import com.example.projectCollab.util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    public UserController(UserRepository userRepository, AuthUtil authUtil) {
        this.userRepository = userRepository;
        this.authUtil = authUtil;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        User user = authUtil.getCurrentUser(authentication);
        return UserResponse.fromEntity(user);
    }
}