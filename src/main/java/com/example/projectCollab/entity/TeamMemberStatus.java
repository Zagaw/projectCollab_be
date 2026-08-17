package com.example.projectCollab.entity;

public enum TeamMemberStatus {
    PENDING,    // Invited but not yet accepted
    ACTIVE,     // Accepted and active in team
    REJECTED,   // Rejected the invitation
    REMOVED     // Removed from team
}