package com.example.projectCollab.repository;

import com.example.projectCollab.entity.File;
import com.example.projectCollab.entity.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findByComment_CommentId(Long commentId);

    @Query("SELECT f FROM File f WHERE f.comment.commentId = :commentId ORDER BY f.uploadedAt ASC")
    List<File> findFilesByCommentIdOrderByUploadedAtAsc(@Param("commentId") Long commentId);

    @Query("SELECT f FROM File f WHERE f.uploadedBy.userId = :userId ORDER BY f.uploadedAt DESC")
    List<File> findFilesByUploadedByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM File f WHERE f.fileName LIKE %:searchTerm%")
    List<File> searchByFileName(@Param("searchTerm") String searchTerm);

    @Query("SELECT COUNT(f) FROM File f WHERE f.comment.commentId = :commentId")
    long countFilesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT SUM(f.fileSize) FROM File f WHERE f.comment.commentId = :commentId")
    Optional<Long> sumFileSizesByCommentId(@Param("commentId") Long commentId);

    List<File> findByProject_ProjectIdAndCommentIsNullOrderByUploadedAtDesc(Long projectId);

    List<File> findByProject_ProjectIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(
            Long projectId, FileCategory category);

    List<File> findByProject_ProjectIdAndTeam_TeamIdAndCommentIsNullOrderByUploadedAtDesc(
            Long projectId, Long teamId);

    List<File> findByProject_ProjectIdAndTeam_TeamIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(
            Long projectId, Long teamId, FileCategory category);

    List<File> findByTeam_TeamIdAndCommentIsNullOrderByUploadedAtDesc(Long teamId);

    List<File> findByTeam_TeamIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(
            Long teamId, FileCategory category);

    @Query("SELECT f.uploadedBy.userId, COUNT(f) FROM File f WHERE f.team.teamId = :teamId GROUP BY f.uploadedBy.userId")
    List<Object[]> countByUploaderForTeam(@Param("teamId") Long teamId);

    @Query("SELECT f.uploadedBy.userId, COUNT(f) FROM File f WHERE f.comment.task.team.teamId = :teamId GROUP BY f.uploadedBy.userId")
    List<Object[]> countByUploaderForTeamTaskComments(@Param("teamId") Long teamId);
}