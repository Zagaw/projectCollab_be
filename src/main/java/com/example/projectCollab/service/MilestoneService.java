package com.example.projectCollab.service;

import com.example.projectCollab.dto.MilestoneRequest;
import com.example.projectCollab.dto.MilestoneResponse;
import com.example.projectCollab.entity.Milestone;
import com.example.projectCollab.entity.Role;
import com.example.projectCollab.entity.Team;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.MilestoneRepository;
import com.example.projectCollab.repository.TeamMemberRepository;
import com.example.projectCollab.repository.TeamRepository;
import com.example.projectCollab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            TeamRepository teamRepository,
                            TeamMemberRepository teamMemberRepository,
                            UserRepository userRepository,
                            ActivityService activityService,
                            NotificationService notificationService) {
        this.milestoneRepository = milestoneRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    // ==========================================
    // CREATE MILESTONE (Team Leader only)
    // ==========================================
    @Transactional
    public MilestoneResponse createMilestone(MilestoneRequest request, Long userId) {
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if team exists
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        // Verify user is the team leader
        if (team.getTeamLeader() == null || !team.getTeamLeader().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Only team leader can create milestones for this team");
        }

        // Create milestone
        Milestone milestone = new Milestone();
        milestone.setTitle(request.getTitle());
        milestone.setDescription(request.getDescription());
        milestone.setDeadline(request.getDeadline());
        milestone.setTeam(team);
        milestone.setCreatedBy(user);

        Milestone savedMilestone = milestoneRepository.save(milestone);
        activityService.logActivity(
                user,
                team.getProject(),
                "MILESTONE_CREATED",
                user.getFirstName() + " " + user.getLastName() + " created milestone " + savedMilestone.getTitle(),
                "MILESTONE",
                savedMilestone.getMilestoneId()
        );
        notificationService.notifyMilestoneCreated(user, team, savedMilestone);
        return convertToResponse(savedMilestone);
    }

    // ==========================================
    // GET MILESTONE BY ID
    // ==========================================
    public MilestoneResponse getMilestoneById(Long milestoneId, Long userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        // Check if user has access to this milestone
        if (!hasAccessToMilestone(milestone, userId)) {
            throw new UnauthorizedAccessException("You don't have access to this milestone");
        }

        return convertToResponse(milestone);
    }

    // ==========================================
    // GET MILESTONES BY TEAM
    // ==========================================
    public List<MilestoneResponse> getMilestonesByTeam(Long teamId, Long userId) {
        if (!canViewTeam(teamId, userId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this team's milestones");
        }

        List<Milestone> milestones = milestoneRepository.findByTeam_TeamIdOrderByDeadlineAsc(teamId);
        return milestones.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MilestoneResponse> getMilestonesForLecturer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.LECTURER && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedAccessException("Only lecturers can view project milestone overview");
        }
        return milestoneRepository.findByLecturerIdWithDetails(userId).stream()
                .sorted((a, b) -> {
                    if (a.getDeadline() == null) return 1;
                    if (b.getDeadline() == null) return -1;
                    return a.getDeadline().compareTo(b.getDeadline());
                })
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET MY TEAM MILESTONES (For team leader)
    // ==========================================
    public List<MilestoneResponse> getMyTeamMilestones(Long userId) {
        // Get all teams where user is team leader
        List<Team> teams = teamRepository.findByTeamLeaderUserId(userId);
        
        if (teams.isEmpty()) {
            return List.of();
        }

        List<Long> teamIds = teams.stream()
                .map(Team::getTeamId)
                .collect(Collectors.toList());

        List<Milestone> milestones = milestoneRepository.findAll().stream()
                .filter(m -> teamIds.contains(m.getTeam().getTeamId()))
                .sorted((m1, m2) -> m1.getDeadline().compareTo(m2.getDeadline()))
                .collect(Collectors.toList());

        return milestones.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET INCOMPLETE MILESTONES
    // ==========================================
    public List<MilestoneResponse> getIncompleteMilestones(Long teamId, Long userId) {
        if (!canViewTeam(teamId, userId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this team's milestones");
        }

        List<Milestone> milestones = milestoneRepository
                .findByTeam_TeamIdAndIsCompletedFalseOrderByDeadlineAsc(teamId);
        return milestones.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET OVERDUE MILESTONES
    // ==========================================
    public List<MilestoneResponse> getOverdueMilestones(Long teamId, Long userId) {
        if (!canViewTeam(teamId, userId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this team's milestones");
        }

        List<Milestone> milestones = milestoneRepository
                .findOverdueMilestones(teamId, LocalDateTime.now());
        return milestones.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // UPDATE MILESTONE (Team Leader only)
    // ==========================================
    @Transactional
    public MilestoneResponse updateMilestone(Long milestoneId, MilestoneRequest request, Long userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        // Verify user is the team leader
        if (!isTeamLeader(milestone.getTeam().getTeamId(), userId)) {
            throw new UnauthorizedAccessException("Only team leader can update milestones");
        }

        // Check if milestone is already completed
        if (milestone.getIsCompleted()) {
            throw new IllegalStateException("Cannot update a completed milestone");
        }

        milestone.setTitle(request.getTitle());
        milestone.setDescription(request.getDescription());
        milestone.setDeadline(request.getDeadline());

        Milestone updatedMilestone = milestoneRepository.save(milestone);
        activityService.logActivity(
                userRepository.findById(userId).orElse(milestone.getCreatedBy()),
                milestone.getTeam().getProject(),
                "MILESTONE_UPDATED",
                "Milestone updated: " + updatedMilestone.getTitle(),
                "MILESTONE",
                updatedMilestone.getMilestoneId()
        );
        return convertToResponse(updatedMilestone);
    }

    // ==========================================
    // UPDATE MILESTONE STATUS (Team Leader only)
    // ==========================================
    @Transactional
    public MilestoneResponse updateMilestoneStatus(Long milestoneId, Boolean isCompleted, Long userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        // Verify user is the team leader
        if (!isTeamLeader(milestone.getTeam().getTeamId(), userId)) {
            throw new UnauthorizedAccessException("Only team leader can update milestone status");
        }

        milestone.setIsCompleted(isCompleted);
        
        if (isCompleted) {
            milestone.setCompletedAt(LocalDateTime.now());
        } else {
            milestone.setCompletedAt(null);
        }

        Milestone updatedMilestone = milestoneRepository.save(milestone);
        activityService.logActivity(
                userRepository.findById(userId).orElse(milestone.getCreatedBy()),
                milestone.getTeam().getProject(),
                "MILESTONE_STATUS_UPDATED",
                "Milestone " + updatedMilestone.getTitle() + " marked " + (isCompleted ? "completed" : "incomplete"),
                "MILESTONE",
                updatedMilestone.getMilestoneId()
        );
        return convertToResponse(updatedMilestone);
    }

    // ==========================================
    // DELETE MILESTONE (Team Leader only)
    // ==========================================
    @Transactional
    public void deleteMilestone(Long milestoneId, Long userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        // Verify user is the team leader
        if (!isTeamLeader(milestone.getTeam().getTeamId(), userId)) {
            throw new UnauthorizedAccessException("Only team leader can delete milestones");
        }

        activityService.logActivity(
                userRepository.findById(userId).orElse(milestone.getCreatedBy()),
                milestone.getTeam().getProject(),
                "MILESTONE_DELETED",
                "Milestone deleted: " + milestone.getTitle(),
                "MILESTONE",
                milestone.getMilestoneId()
        );

        milestoneRepository.delete(milestone);
    }

    // ==========================================
    // GET MILESTONE STATISTICS (Team Leader only)
    // ==========================================
    public MilestoneStatistics getMilestoneStatistics(Long teamId, Long userId) {
        if (!canViewTeam(teamId, userId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this team's milestones");
        }

        long total = milestoneRepository.count();
        long completed = milestoneRepository.countCompletedMilestones(teamId);
        long incomplete = milestoneRepository.countIncompleteMilestones(teamId);
        long overdue = milestoneRepository.findOverdueMilestones(teamId, LocalDateTime.now()).size();

        MilestoneStatistics stats = new MilestoneStatistics();
        stats.setTotalMilestones(total);
        stats.setCompletedMilestones(completed);
        stats.setIncompleteMilestones(incomplete);
        stats.setOverdueMilestones(overdue);
        
        if (total > 0) {
            stats.setCompletionPercentage((completed * 100.0) / total);
        } else {
            stats.setCompletionPercentage(0.0);
        }

        return stats;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private boolean isTeamMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeam_TeamIdAndUser_UserId(teamId, userId).isPresent();
    }

    private boolean isTeamLeader(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        return team != null && team.getTeamLeader() != null && 
               team.getTeamLeader().getUserId().equals(userId);
    }

    private boolean canViewTeam(Long teamId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return false;
        }
        if (team.getProject() != null && team.getProject().getLecturer() != null
                && team.getProject().getLecturer().getUserId().equals(userId)) {
            return true;
        }
        return isTeamLeader(teamId, userId) || isTeamMember(teamId, userId);
    }

    private boolean hasAccessToMilestone(Milestone milestone, Long userId) {
        return canViewTeam(milestone.getTeam().getTeamId(), userId);
    }

    private MilestoneResponse convertToResponse(Milestone milestone) {
        MilestoneResponse response = new MilestoneResponse();
        response.setMilestoneId(milestone.getMilestoneId());
        response.setTitle(milestone.getTitle());
        response.setDescription(milestone.getDescription());
        response.setDeadline(milestone.getDeadline());
        response.setIsCompleted(milestone.getIsCompleted());
        response.setCompletedAt(milestone.getCompletedAt());
        response.setCreatedAt(milestone.getCreatedAt());
        response.setUpdatedAt(milestone.getUpdatedAt());
        response.setTeamId(milestone.getTeam().getTeamId());
        response.setTeamName(milestone.getTeam().getName());
        response.setCreatedBy(milestone.getCreatedBy().getUserId());
        response.setCreatedByName(milestone.getCreatedBy().getFirstName() + " " + 
                                 milestone.getCreatedBy().getLastName());
        
        if (milestone.getTeam().getProject() != null) {
            response.setProjectId(milestone.getTeam().getProject().getProjectId());
            response.setProjectTitle(milestone.getTeam().getProject().getTitle());
        }
        
        return response;
    }

    // ==========================================
    // INNER CLASS FOR STATISTICS
    // ==========================================
    public static class MilestoneStatistics {
        private long totalMilestones;
        private long completedMilestones;
        private long incompleteMilestones;
        private long overdueMilestones;
        private double completionPercentage;

        // Getters and Setters
        public long getTotalMilestones() { return totalMilestones; }
        public void setTotalMilestones(long totalMilestones) { this.totalMilestones = totalMilestones; }
        public long getCompletedMilestones() { return completedMilestones; }
        public void setCompletedMilestones(long completedMilestones) { this.completedMilestones = completedMilestones; }
        public long getIncompleteMilestones() { return incompleteMilestones; }
        public void setIncompleteMilestones(long incompleteMilestones) { this.incompleteMilestones = incompleteMilestones; }
        public long getOverdueMilestones() { return overdueMilestones; }
        public void setOverdueMilestones(long overdueMilestones) { this.overdueMilestones = overdueMilestones; }
        public double getCompletionPercentage() { return completionPercentage; }
        public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
    }
}