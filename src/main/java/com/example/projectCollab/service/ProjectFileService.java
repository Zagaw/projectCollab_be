package com.example.projectCollab.service;

import com.example.projectCollab.dto.FileResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.FileStorageException;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectFileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final FileVersionService fileVersionService;
    private final ProjectAccessService projectAccessService;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    @Transactional
    public FileResponse uploadToProject(Long projectId, MultipartFile file, String category,
                                        Long teamId, User currentUser) {
        projectAccessService.requireProjectAccess(projectId, currentUser);
        Project project = projectAccessService.requireProject(projectId);

        Team team = projectAccessService.resolveLibraryUploadTeam(project, teamId, currentUser);
        return storeLibraryFile(file, project, team, parseCategory(category), currentUser);
    }

    @Transactional
    public FileResponse uploadToTeam(Long teamId, MultipartFile file, String category, User currentUser) {
        projectAccessService.requireTeamAccess(teamId, currentUser);
        Team team = projectAccessService.requireTeam(teamId);
        return storeLibraryFile(file, team.getProject(), team, parseCategory(category), currentUser);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listProjectFiles(Long projectId, String category, Long teamId, User currentUser) {
        projectAccessService.requireProjectAccess(projectId, currentUser);
        FileCategory cat = parseOptionalCategory(category);

        List<File> files;
        if (teamId != null && cat != null) {
            files = fileRepository.findByProject_ProjectIdAndTeam_TeamIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(
                    projectId, teamId, cat);
        } else if (teamId != null) {
            files = fileRepository.findByProject_ProjectIdAndTeam_TeamIdAndCommentIsNullOrderByUploadedAtDesc(
                    projectId, teamId);
        } else if (cat != null) {
            files = fileRepository.findByProject_ProjectIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(
                    projectId, cat);
        } else {
            files = fileRepository.findByProject_ProjectIdAndCommentIsNullOrderByUploadedAtDesc(projectId);
        }

        return files.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listTeamFiles(Long teamId, String category, User currentUser) {
        projectAccessService.requireTeamAccess(teamId, currentUser);
        FileCategory cat = parseOptionalCategory(category);
        List<File> files = cat != null
                ? fileRepository.findByTeam_TeamIdAndCategoryAndCommentIsNullOrderByUploadedAtDesc(teamId, cat)
                : fileRepository.findByTeam_TeamIdAndCommentIsNullOrderByUploadedAtDesc(teamId);
        return files.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long fileId, User currentUser) throws IOException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!canDelete(file, currentUser)) {
            throw new UnauthorizedAccessException("You don't have permission to delete this file");
        }

        fileStorageService.deleteFile(fileId);
    }

    public FileResponse mapToResponse(File file) {
        FileResponse response = new FileResponse();
        response.setFileId(file.getFileId());
        response.setFileName(file.getFileName());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getFileType());
        response.setFileUrl("/api/files/download/" + file.getFileId());
        response.setUploadedAt(file.getUploadedAt());
        if (file.getUploadedBy() != null) {
            response.setUploadedBy(file.getUploadedBy().getUserId());
            response.setUploadedByName(file.getUploadedBy().getFirstName() + " "
                    + file.getUploadedBy().getLastName());
        }
        response.setStorageType(file.getStorageType() != null ? file.getStorageType().name() : "LOCAL");
        response.setVersionNumber(file.getCurrentVersion());
        response.setCategory(file.getCategory() != null ? file.getCategory().name() : FileCategory.GENERAL.name());
        if (file.getProject() != null) {
            response.setProjectId(file.getProject().getProjectId());
        }
        if (file.getTeam() != null) {
            response.setTeamId(file.getTeam().getTeamId());
            response.setTeamName(file.getTeam().getName());
        }
        if (file.getComment() != null) {
            response.setCommentId(file.getComment().getCommentId());
        }
        return response;
    }

    private FileResponse storeLibraryFile(MultipartFile file, Project project, Team team,
                                          FileCategory category, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        try {
            File fileEntity = fileStorageService.prepareFileEntity(file, currentUser);
            fileEntity.setProject(project);
            fileEntity.setTeam(team);
            fileEntity.setCategory(category);
            File savedFile = fileRepository.save(fileEntity);
            fileVersionService.createInitialVersion(savedFile, file, currentUser);

            String description = currentUser.getFirstName() + " " + currentUser.getLastName()
                    + " uploaded file: " + savedFile.getFileName();
            activityService.logActivity(
                    currentUser,
                    project,
                    "FILE_UPLOADED",
                    description,
                    "FILE",
                    savedFile.getFileId()
            );
            notificationService.notifyFileUploaded(currentUser, project, savedFile);

            return mapToResponse(savedFile);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    private boolean canDelete(File file, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (file.getUploadedBy() != null
                && file.getUploadedBy().getUserId().equals(currentUser.getUserId())) {
            return true;
        }
        Project project = resolveProject(file);
        if (project != null && project.getLecturer() != null
                && project.getLecturer().getUserId().equals(currentUser.getUserId())) {
            return true;
        }
        if (file.getTeam() != null && file.getTeam().getTeamLeader() != null
                && file.getTeam().getTeamLeader().getUserId().equals(currentUser.getUserId())) {
            return true;
        }
        return false;
    }

    private Project resolveProject(File file) {
        if (file.getProject() != null) {
            return file.getProject();
        }
        if (file.getTeam() != null) {
            return file.getTeam().getProject();
        }
        if (file.getComment() != null) {
            if (file.getComment().getProject() != null) {
                return file.getComment().getProject();
            }
            if (file.getComment().getTask() != null) {
                return file.getComment().getTask().getProject();
            }
        }
        return null;
    }

    private FileCategory parseCategory(String category) {
        FileCategory parsed = parseOptionalCategory(category);
        return parsed != null ? parsed : FileCategory.GENERAL;
    }

    private FileCategory parseOptionalCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        try {
            return FileCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category. Allowed values: GENERAL, REPORT, DESIGN, SUBMISSION");
        }
    }
}
