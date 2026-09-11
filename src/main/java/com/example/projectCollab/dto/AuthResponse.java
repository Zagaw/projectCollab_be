package com.example.projectCollab.dto;

public record AuthResponse(

        String token,

        String tokenType,

        Long userId,

        String username,

        String email,

        String firstName,

        String lastName,

        String role,

        String status

) {
}