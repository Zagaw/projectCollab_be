package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Query("SELECT DISTINCT m FROM Meeting m JOIN FETCH m.team t JOIN FETCH t.project p LEFT JOIN FETCH t.teamLeader LEFT JOIN FETCH m.createdBy WHERE t.teamId = :teamId ORDER BY m.startAt ASC")
    List<Meeting> findByTeamIdWithDetails(@Param("teamId") Long teamId);

    @Query("SELECT DISTINCT m FROM Meeting m JOIN FETCH m.team t JOIN FETCH t.project p LEFT JOIN FETCH t.teamLeader LEFT JOIN FETCH m.createdBy WHERE t.teamId IN :teamIds ORDER BY m.startAt ASC")
    List<Meeting> findByTeamIdInWithDetails(@Param("teamIds") List<Long> teamIds);

    @Query("SELECT DISTINCT m FROM Meeting m JOIN FETCH m.team t JOIN FETCH t.project p LEFT JOIN FETCH t.teamLeader LEFT JOIN FETCH m.createdBy WHERE p.lecturer.userId = :lecturerId ORDER BY m.startAt ASC")
    List<Meeting> findByLecturerIdWithDetails(@Param("lecturerId") Long lecturerId);

    @Query("SELECT m FROM Meeting m JOIN FETCH m.team t JOIN FETCH t.project p LEFT JOIN FETCH t.teamLeader LEFT JOIN FETCH m.createdBy WHERE m.meetingId = :meetingId")
    Optional<Meeting> findByIdWithDetails(@Param("meetingId") Long meetingId);
}
