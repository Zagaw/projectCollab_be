package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // ✅ Use @Query for all methods to avoid naming issues
    @Query("SELECT a FROM Activity a WHERE a.project.projectId = :projectId ORDER BY a.createdAt DESC")
    List<Activity> findActivitiesByProject(@Param("projectId") Long projectId);

    @Query("SELECT a FROM Activity a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<Activity> findActivitiesByUser(@Param("userId") Long userId);

    @Query("SELECT a FROM Activity a WHERE a.project.projectId = :projectId AND a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<Activity> findActivitiesByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT a FROM Activity a WHERE a.project.projectId = :projectId ORDER BY a.createdAt DESC LIMIT :limit")
    List<Activity> findRecentActivitiesByProject(@Param("projectId") Long projectId, @Param("limit") int limit);

    @Query("SELECT a FROM Activity a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC LIMIT :limit")
    List<Activity> findRecentActivitiesByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user LEFT JOIN FETCH a.project " +
            "WHERE a.project.projectId = :projectId AND a.createdAt >= :from AND a.createdAt <= :to " +
            "ORDER BY a.createdAt DESC")
    List<Activity> findByProjectAndCreatedAtBetween(
            @Param("projectId") Long projectId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user LEFT JOIN FETCH a.project " +
            "WHERE a.project.projectId = :projectId AND a.user.userId IN :userIds " +
            "AND a.createdAt >= :from AND a.createdAt <= :to ORDER BY a.createdAt DESC")
    List<Activity> findByProjectUsersAndCreatedAtBetween(
            @Param("projectId") Long projectId,
            @Param("userIds") List<Long> userIds,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);
}