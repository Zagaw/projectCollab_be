package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Get active comments for a task (excluding soft-deleted)
    @Query("SELECT c FROM Comment c WHERE c.task.taskId = :taskId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveCommentsByTask(@Param("taskId") Long taskId);

    // Get active comments for a project
    @Query("SELECT c FROM Comment c WHERE c.project.projectId = :projectId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveCommentsByProject(@Param("projectId") Long projectId);

    // Get active root comments for a task (no parent)
    @Query("SELECT c FROM Comment c WHERE c.task.taskId = :taskId AND c.parentComment IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveRootCommentsByTask(@Param("taskId") Long taskId);

    // Get active replies for a parent comment
    @Query("SELECT c FROM Comment c WHERE c.parentComment.commentId = :parentId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveRepliesByParentCommentId(@Param("parentId") Long parentId);

    // Count active comments for a task
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.task.taskId = :taskId AND c.isDeleted = false")
    long countActiveByTaskId(@Param("taskId") Long taskId);
}