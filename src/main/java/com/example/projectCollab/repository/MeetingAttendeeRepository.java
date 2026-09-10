package com.example.projectCollab.repository;

import com.example.projectCollab.entity.MeetingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> {

    Optional<MeetingAttendee> findByMeeting_MeetingIdAndUser_UserId(Long meetingId, Long userId);

    @Query("SELECT a FROM MeetingAttendee a JOIN FETCH a.user WHERE a.meeting.meetingId = :meetingId ORDER BY a.respondedAt ASC")
    List<MeetingAttendee> findByMeetingIdWithUser(@Param("meetingId") Long meetingId);
}
