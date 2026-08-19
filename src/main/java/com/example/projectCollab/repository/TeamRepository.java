package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByProject_ProjectId(Long projectId);
    List<Team> findByTeamLeaderUserId(Long userId);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.user.userId = :userId AND m.status = 'ACTIVE'")
    List<Team> findTeamsByMemberId(@Param("userId") Long userId);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.user.userId = :userId AND m.status = 'PENDING'")
    List<Team> findPendingInvitations(@Param("userId") Long userId);
}