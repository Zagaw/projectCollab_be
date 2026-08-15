package com.example.projectCollab.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Admin123";  // Change this to your desired password
        String hashedPassword = encoder.encode(password);
        System.out.println("Hashed Password: " + hashedPassword);
        System.out.println("Use this password: " + password);
    }
}