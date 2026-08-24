package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    List<Discussion> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT d FROM Discussion d WHERE d.project.projectId = :projectId AND d.createdBy.userId = :userId")
    List<Discussion> findByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    long countByProjectProjectId(Long projectId);
}