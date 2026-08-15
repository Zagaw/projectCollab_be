package com.example.projectCollab.service;

import com.example.projectCollab.entity.User;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // This is a placeholder - implement with actual email service (JavaMailSender)
    public void sendVerificationEmail(User user) {
        // Send email to user that their account is pending verification
        System.out.println("📧 Sending verification email to: " + user.getEmail());
        System.out.println("Your lecturer account is pending admin approval.");
    }

    public void notifyAdmins(User newLecturer) {
        // Notify all admins about new lecturer registration
        System.out.println("📧 New lecturer registration pending: " + newLecturer.getEmail());
        System.out.println("Please verify at: /api/admin/pending-lecturers");
    }
}