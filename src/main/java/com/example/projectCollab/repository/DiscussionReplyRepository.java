package com.example.projectCollab.repository;

import com.example.projectCollab.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    List<DiscussionReply> findByDiscussionDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    long countByDiscussionDiscussionId(Long discussionId);
}