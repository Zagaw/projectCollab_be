package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTaskTaskIdOrderByCreatedAtAsc(Long taskId);

    List<Comment> findByProjectProjectIdOrderByCreatedAtAsc(Long projectId);

    List<Comment> findByParentCommentIsNullAndTaskTaskIdOrderByCreatedAtAsc(Long taskId);

    @Query("SELECT c FROM Comment c WHERE c.task.taskId = :taskId AND c.parentComment IS NULL")
    List<Comment> findRootCommentsByTask(@Param("taskId") Long taskId);

    @Query("SELECT c FROM Comment c WHERE c.parentComment.commentId = :parentId")
    List<Comment> findRepliesByParentCommentId(@Param("parentId") Long parentId);

    long countByTaskTaskId(Long taskId);
}