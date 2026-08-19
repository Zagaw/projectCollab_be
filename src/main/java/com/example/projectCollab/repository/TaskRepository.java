package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Task;
import com.example.projectCollab.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectProjectId(Long projectId);

    List<Task> findByTeamTeamId(Long teamId);

    List<Task> findByAssignedToUserId(Long userId);

    List<Task> findByProjectProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Task> findByAssignedToUserIdAndStatus(Long userId, TaskStatus status);

    List<Task> findByMilestoneMilestoneId(Long milestoneId);

    @Query("SELECT t FROM Task t WHERE t.deadline < :now AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.projectId = :projectId AND t.status = 'COMPLETED'")
    Long countCompletedTasksByProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.projectId = :projectId")
    Long countTotalTasksByProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.team.teamId = :teamId AND t.status = 'COMPLETED'")
    Long countCompletedTasksByTeam(@Param("teamId") Long teamId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.team.teamId = :teamId")
    Long countTotalTasksByTeam(@Param("teamId") Long teamId);

    @Query("SELECT t FROM Task t WHERE t.assignedTo.userId = :userId AND t.deadline < :now AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasksForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}