package com.example.projectCollab.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "meeting_attendees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"})
)
public class MeetingAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingRsvpResponse response;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        respondedAt = LocalDateTime.now();
    }

    public Long getAttendeeId() { return attendeeId; }
    public void setAttendeeId(Long attendeeId) { this.attendeeId = attendeeId; }

    public Meeting getMeeting() { return meeting; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MeetingRsvpResponse getResponse() { return response; }
    public void setResponse(MeetingRsvpResponse response) { this.response = response; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
}
