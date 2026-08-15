package com.example.projectCollab.dto;

import com.example.projectCollab.entity.User;

public record UserResponse(

        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String studentId,
        String phone,
        String profileImage,
        String role,
        String status

) {

    public static UserResponse fromEntity(User user) {

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStudentId(),
                user.getPhone(),
                user.getProfileImage(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
