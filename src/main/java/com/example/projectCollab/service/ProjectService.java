package com.example.projectCollab.service;

import com.example.projectCollab.dto.ProjectRequest;
import com.example.projectCollab.dto.ProjectResponse;
import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.ProjectStatus;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // CREATE PROJECT
    // ==========================================

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Long lecturerId) {
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        // Validate dates
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        Project project = new Project();
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setCourse(request.course());
        project.setSemester(request.semester());
        project.setLecturer(lecturer);
        project.setStatus(ProjectStatus.ACTIVE);

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    // ==========================================
    // GET PROJECT BY ID
    // ==========================================

    public ProjectResponse getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return ProjectResponse.fromEntity(project);
    }

    // ==========================================
    // GET ALL PROJECTS BY LECTURER
    // ==========================================

    public List<ProjectResponse> getProjectsByLecturer(Long lecturerId) {
        List<Project> projects = projectRepository.findByLecturer_UserId(lecturerId);
        return projects.stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET ALL PROJECTS
    // ==========================================

    public List<ProjectResponse> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // UPDATE PROJECT
    // ==========================================

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long lecturerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify lecturer owns this project
        if (!project.getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to update this project");
        }

        // Validate dates
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setCourse(request.course());
        project.setSemester(request.semester());

        Project updatedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(updatedProject);
    }

    // ==========================================
    // DELETE PROJECT
    // ==========================================

    @Transactional
    public void deleteProject(Long projectId, Long lecturerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify lecturer owns this project
        if (!project.getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to delete this project");
        }

        // Check if project has teams
        if (!project.getTeams().isEmpty()) {
            throw new IllegalStateException("Cannot delete project with existing teams. Remove teams first.");
        }

        projectRepository.delete(project);
    }

    // ==========================================
    // UPDATE PROJECT STATUS
    // ==========================================

    @Transactional
    public ProjectResponse updateProjectStatus(Long projectId, ProjectStatus status, Long lecturerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify lecturer owns this project
        if (!project.getLecturer().getUserId().equals(lecturerId)) {
            throw new RuntimeException("You don't have permission to update this project");
        }

        project.setStatus(status);
        Project updatedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(updatedProject);
    }

    // ==========================================
    // GET OVERDUE PROJECTS
    // ==========================================

    public List<ProjectResponse> getOverdueProjects() {
        List<Project> overdue = projectRepository.findOverdueProjects(LocalDateTime.now());
        return overdue.stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }
}