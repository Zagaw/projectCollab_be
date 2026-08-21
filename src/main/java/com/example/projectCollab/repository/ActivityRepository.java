package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Activity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Activity> findByProjectProjectIdAndUserIdOrderByCreatedAtDesc(Long projectId, Long userId);

    @Query("SELECT a FROM Activity a WHERE a.project.projectId = :projectId ORDER BY a.createdAt DESC LIMIT :limit")
    List<Activity> findRecentActivitiesByProject(@Param("projectId") Long projectId, @Param("limit") int limit);

    @Query("SELECT a FROM Activity a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC LIMIT :limit")
    List<Activity> findRecentActivitiesByUser(@Param("userId") Long userId, @Param("limit") int limit);
}