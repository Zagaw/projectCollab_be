package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByTeam_TeamId(Long teamId);

    List<Milestone> findByTeam_TeamIdOrderByDeadlineAsc(Long teamId);

    List<Milestone> findByTeam_TeamIdAndIsCompletedFalseOrderByDeadlineAsc(Long teamId);

    List<Milestone> findByTeam_TeamIdAndIsCompletedTrueOrderByCompletedAtDesc(Long teamId);

    @Query("SELECT m FROM Milestone m WHERE m.team.teamId = :teamId AND m.deadline < :now AND m.isCompleted = false")
    List<Milestone> findOverdueMilestones(@Param("teamId") Long teamId, @Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT m FROM Milestone m "
            + "JOIN FETCH m.team tm "
            + "LEFT JOIN FETCH tm.teamLeader "
            + "LEFT JOIN FETCH tm.project p "
            + "LEFT JOIN FETCH p.lecturer "
            + "WHERE m.deadline < :now AND m.isCompleted = false")
    List<Milestone> findOverdueMilestonesWithRelations(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT m FROM Milestone m "
            + "JOIN FETCH m.team tm "
            + "LEFT JOIN FETCH tm.teamLeader "
            + "LEFT JOIN FETCH tm.project p "
            + "LEFT JOIN FETCH p.lecturer "
            + "WHERE m.deadline >= :now AND m.deadline <= :until AND m.isCompleted = false")
    List<Milestone> findDueSoonMilestonesWithRelations(@Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

    @Query("SELECT m FROM Milestone m WHERE m.team.project.lecturer.userId = :lecturerId")
    List<Milestone> findByLecturerId(@Param("lecturerId") Long lecturerId);

    @Query("SELECT DISTINCT m FROM Milestone m JOIN FETCH m.team t JOIN FETCH t.project p LEFT JOIN FETCH m.createdBy WHERE p.lecturer.userId = :lecturerId")
    List<Milestone> findByLecturerIdWithDetails(@Param("lecturerId") Long lecturerId);

    @Query("SELECT m FROM Milestone m WHERE m.createdBy.userId = :userId")
    List<Milestone> findByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Milestone m WHERE m.team.teamId = :teamId AND m.isCompleted = false")
    long countIncompleteMilestones(@Param("teamId") Long teamId);

    @Query("SELECT COUNT(m) FROM Milestone m WHERE m.team.teamId = :teamId AND m.isCompleted = true")
    long countCompletedMilestones(@Param("teamId") Long teamId);
}