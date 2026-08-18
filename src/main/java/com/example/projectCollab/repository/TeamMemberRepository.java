package com.example.projectCollab.repository;

import com.example.projectCollab.entity.TeamMember;
import com.example.projectCollab.entity.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // ==========================================
    // FIND BY USER
    // ==========================================

    // Get all team memberships for a user
    List<TeamMember> findByUser_UserId(Long userId);

    // Get pending invitations for a user - FIXED with JOIN FETCH
    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.team t " +
            "JOIN FETCH t.project p " +
            "WHERE tm.user.userId = :userId AND tm.status = :status")
    List<TeamMember> findByUser_UserIdAndStatus(@Param("userId") Long userId,
                                                @Param("status") TeamMemberStatus status);

    // ==========================================
    // FIND BY TEAM
    // ==========================================

    // Get all members of a team - FIXED: Removed duplicate, only keep the @Query version
    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.team t " +
            "JOIN FETCH tm.user u " +
            "WHERE t.teamId = :teamId")
    List<TeamMember> findMembersByTeamId(@Param("teamId") Long teamId);

    // Alternative: Use this if you prefer the default JPA method
    // List<TeamMember> findByTeam_TeamId(Long teamId);

    // Get active members of a team
    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.user u " +
            "WHERE tm.team.teamId = :teamId AND tm.status = 'ACTIVE'")
    List<TeamMember> findActiveMembersByTeamId(@Param("teamId") Long teamId);

    // Get pending members of a team
    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.user u " +
            "WHERE tm.team.teamId = :teamId AND tm.status = 'PENDING'")
    List<TeamMember> findPendingMembers(@Param("teamId") Long teamId);

    // ==========================================
    // FIND BY TEAM AND USER
    // ==========================================

    // Check if a user is a member of a team
    @Query("SELECT tm FROM TeamMember tm " +
            "WHERE tm.team.teamId = :teamId AND tm.user.userId = :userId")
    Optional<TeamMember> findByTeam_TeamIdAndUser_UserId(@Param("teamId") Long teamId,
                                                         @Param("userId") Long userId);

    // Check if a user exists in a team (boolean check - more efficient)
    boolean existsByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);

    // ==========================================
    // ADD THESE METHODS FOR MILESTONE ACCESS
    // ==========================================

    // Check if a user has an ACTIVE membership in a team
    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END " +
            "FROM TeamMember tm " +
            "WHERE tm.team.teamId = :teamId " +
            "AND tm.user.userId = :userId " +
            "AND tm.status = 'ACTIVE'")
    boolean isActiveMember(@Param("teamId") Long teamId, 
                          @Param("userId") Long userId);

    // Get active team memberships for a user (JOIN FETCH for efficiency)
    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.team t " +
            "JOIN FETCH t.project p " +
            "WHERE tm.user.userId = :userId AND tm.status = 'ACTIVE'")
    List<TeamMember> findActiveTeamsByUserId(@Param("userId") Long userId);

    // Get all teams where user is a member (any status)
    @Query("SELECT tm.team.teamId FROM TeamMember tm " +
            "WHERE tm.user.userId = :userId AND tm.status = 'ACTIVE'")
    List<Long> findActiveTeamIdsByUserId(@Param("userId") Long userId);

    // ==========================================
    // COUNT QUERIES
    // ==========================================

    // Count active members in a team
    @Query("SELECT COUNT(tm) FROM TeamMember tm " +
            "WHERE tm.team.teamId = :teamId AND tm.status = 'ACTIVE'")
    int countActiveMembers(@Param("teamId") Long teamId);

    // Count pending members in a team
    @Query("SELECT COUNT(tm) FROM TeamMember tm " +
            "WHERE tm.team.teamId = :teamId AND tm.status = 'PENDING'")
    int countPendingMembers(@Param("teamId") Long teamId);

    // ==========================================
    // UPDATE QUERIES
    // ==========================================

    // Update member status
    @Modifying
    @Transactional
    @Query("UPDATE TeamMember m SET m.status = :status, m.joinedAt = CURRENT_TIMESTAMP " +
            "WHERE m.teamMemberId = :memberId")
    int updateMemberStatus(@Param("memberId") Long memberId,
                           @Param("status") TeamMemberStatus status);

    // ==========================================
    // DELETE QUERIES
    // ==========================================

    // Remove all members from a team
    @Modifying
    @Transactional
    @Query("DELETE FROM TeamMember tm WHERE tm.team.teamId = :teamId")
    void deleteAllByTeamId(@Param("teamId") Long teamId);
}