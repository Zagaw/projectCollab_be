package com.example.projectCollab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"team_id", "user_id"})
        })
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamMemberStatus status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
        if (status == null) {
            status = TeamMemberStatus.PENDING;
        }
    }

    // Getters and Setters
    public Long getTeamMemberId() { return teamMemberId; }
    public void setTeamMemberId(Long teamMemberId) { this.teamMemberId = teamMemberId; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TeamMemberStatus getStatus() { return status; }
    public void setStatus(TeamMemberStatus status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
}