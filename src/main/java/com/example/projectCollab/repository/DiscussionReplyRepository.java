package com.example.projectCollab.repository;

import com.example.projectCollab.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    List<DiscussionReply> findByDiscussionDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    long countByDiscussionDiscussionId(Long discussionId);

    @Query("SELECT r.user.userId, COUNT(r) FROM DiscussionReply r WHERE r.discussion.project.projectId = :projectId GROUP BY r.user.userId")
    List<Object[]> countByUserForProject(@Param("projectId") Long projectId);
}