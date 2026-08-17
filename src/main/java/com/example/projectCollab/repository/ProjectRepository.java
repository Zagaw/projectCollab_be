package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Project;
import com.example.projectCollab.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByLecturer_UserId(Long lecturerId);

    List<Project> findByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.lecturer.userId = :lecturerId AND p.status = :status")
    List<Project> findByLecturerIdAndStatus(@Param("lecturerId") Long lecturerId,
                                            @Param("status") ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.endDate < :now AND p.status != 'COMPLETED'")
    List<Project> findOverdueProjects(@Param("now") LocalDateTime now);
}