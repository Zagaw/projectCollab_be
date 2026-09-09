package com.example.projectCollab.service;

import com.example.projectCollab.dto.CommentRequest;
import com.example.projectCollab.dto.CommentResponse;
import com.example.projectCollab.dto.FileResponse;
import com.example.projectCollab.entity.*;
import com.example.projectCollab.exception.FileStorageException;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.exception.UnauthorizedAccessException;
import com.example.projectCollab.repository.CommentRepository;
import com.example.projectCollab.repository.FileRepository;
import com.example.projectCollab.repository.TaskRepository;
import com.example.projectCollab.repository.ProjectRepository;
import com.example.projectCollab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final FileVersionService fileVersionService;
    private final NotificationService notificationService;
    private final ProjectAccessService projectAccessService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    // ==========================================
// ADD COMMENT TO TASK WITH FILES - FIXED
// ==========================================
    @Transactional
    public CommentResponse addCommentToTask(Long taskId, CommentRequest request) throws IOException {
        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setTask(task);
        comment.setUser(currentUser);
        comment.setDeleted(false);

        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parentComment);
        }

        // ✅ STEP 1: Save comment first
        Comment savedComment = commentRepository.save(comment);

        // ✅ STEP 2: Process file uploads AFTER comment is saved
        List<File> uploadedFiles = new ArrayList<>();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            for (MultipartFile file : request.getFiles()) {
                if (!file.isEmpty()) {
                    try {
                        // Prepare file entity (does not save to DB)
                        File fileEntity = fileStorageService.prepareFileEntity(file, currentUser);

                        // ✅ Set the comment on the file entity
                        fileEntity.setComment(savedComment);

                        // ✅ Save file to database
                        File savedFile = fileRepository.save(fileEntity);

                        // ✅ Create initial version
                        fileVersionService.createInitialVersion(savedFile, file, currentUser);

                        uploadedFiles.add(savedFile);
                    } catch (IOException e) {
                        throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        savedComment.setFiles(uploadedFiles);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " commented on task: " + task.getTitle() +
                (uploadedFiles.isEmpty() ? "" : " with " + uploadedFiles.size() + " file(s)");
        activityService.logActivity(
                currentUser,
                task.getProject(),
                "COMMENT_ADDED",
                description,
                "TASK",
                taskId
        );
        notificationService.notifyCommentOnTask(currentUser, task, notificationService.clip(request.getContent()));

        return mapToResponse(savedComment);
    }

    // ==========================================
// ADD COMMENT TO PROJECT WITH FILES - FIXED
// ==========================================
    @Transactional
    public CommentResponse addCommentToProject(Long projectId, CommentRequest request) throws IOException {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setProject(project);
        comment.setUser(currentUser);
        comment.setDeleted(false);

        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            comment.setParentComment(parentComment);
        }

        // ✅ STEP 1: Save comment first
        Comment savedComment = commentRepository.save(comment);

        // ✅ STEP 2: Process file uploads AFTER comment is saved
        List<File> uploadedFiles = new ArrayList<>();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            for (MultipartFile file : request.getFiles()) {
                if (!file.isEmpty()) {
                    try {
                        // Prepare file entity (does not save to DB)
                        File fileEntity = fileStorageService.prepareFileEntity(file, currentUser);

                        // ✅ Set the comment on the file entity
                        fileEntity.setComment(savedComment);
                        fileEntity.setProject(project);
                        fileEntity.setCategory(FileCategory.GENERAL);

                        // ✅ Save file to database
                        File savedFile = fileRepository.save(fileEntity);

                        // ✅ Create initial version
                        fileVersionService.createInitialVersion(savedFile, file, currentUser);

                        uploadedFiles.add(savedFile);
                    } catch (IOException e) {
                        throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        savedComment.setFiles(uploadedFiles);

        // Log activity
        String description = currentUser.getFirstName() + " " + currentUser.getLastName() +
                " commented on project: " + project.getTitle() +
                (uploadedFiles.isEmpty() ? "" : " with " + uploadedFiles.size() + " file(s)");
        activityService.logActivity(
                currentUser,
                project,
                "PROJECT_COMMENT_ADDED",
                description,
                "PROJECT",
                projectId
        );
        notificationService.notifyCommentOnProject(currentUser, project, notificationService.clip(request.getContent()));

        return mapToResponse(savedComment);
    }

    // ==========================================
    // GET COMMENTS FOR TASK WITH FILES
    // ==========================================
    public List<CommentResponse> getCommentsForTask(Long taskId) {
        List<Comment> comments = commentRepository.findActiveRootCommentsByTask(taskId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET COMMENTS FOR PROJECT WITH FILES
    // ==========================================
    public List<CommentResponse> getCommentsForProject(Long projectId) {
        List<Comment> comments = commentRepository.findActiveCommentsByProject(projectId);
        return comments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET REPLIES FOR COMMENT
    // ==========================================
    public List<CommentResponse> getRepliesForComment(Long commentId) {
        List<Comment> replies = commentRepository.findActiveRepliesByParentCommentId(commentId);
        return replies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET COMMENT BY ID
    // ==========================================
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (comment.isDeleted()) {
            throw new ResourceNotFoundException("Comment has been deleted");
        }

        return mapToResponse(comment);
    }

    // ==========================================
    // UPDATE COMMENT
    // ==========================================
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedAccessException("You are not authorized to edit this comment");
        }

        if (comment.isDeleted()) {
            throw new RuntimeException("Cannot edit a deleted comment");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);

        return mapToResponse(updatedComment);
    }

    // ==========================================
    // SOFT DELETE COMMENT (with files)
    // ==========================================
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Check authorization
        boolean isOwner = comment.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
        boolean isLecturer = "LECTURER".equals(currentUser.getRole().name());

        if (!isOwner && !isAdmin && !isLecturer) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment");
        }

        // Soft delete - just mark as deleted (files remain)
        comment.setDeleted(true);
        commentRepository.save(comment);
    }

    // ==========================================
    // HARD DELETE COMMENT (with files)
    // ==========================================
    @Transactional
    public void hardDeleteComment(Long commentId) throws IOException {
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole().name())) {
            throw new UnauthorizedAccessException("Only admin can permanently delete comments");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Delete physical files
        if (comment.getFiles() != null && !comment.getFiles().isEmpty()) {
            for (File file : comment.getFiles()) {
                try {
                    fileStorageService.deleteFile(file.getFileId());
                } catch (IOException e) {
                    // Log error but continue deleting other files
                    System.err.println("Failed to delete file: " + file.getFileId() + " - " + e.getMessage());
                }
            }
        }

        // Delete comment (cascade will delete file references from DB)
        commentRepository.delete(comment);
    }

    // ==========================================
    // DELETE FILE FROM COMMENT
    // ==========================================
    @Transactional
    public void deleteFileFromComment(Long fileId) throws IOException {
        User currentUser = getCurrentUser();
        
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Comment comment = file.getComment();
        if (comment == null) {
            throw new ResourceNotFoundException("This file is not attached to a comment");
        }

        // Check if user owns the file or the comment
        boolean isFileOwner = file.getUploadedBy().getUserId().equals(currentUser.getUserId());
        boolean isCommentOwner = comment.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole().name());
        boolean isLecturer = "LECTURER".equals(currentUser.getRole().name());

        if (!isFileOwner && !isCommentOwner && !isAdmin && !isLecturer) {
            throw new UnauthorizedAccessException("You don't have permission to delete this file");
        }

        // Check if comment is deleted
        if (Boolean.TRUE.equals(comment.isDeleted())) {
            throw new RuntimeException("Cannot delete file from a deleted comment");
        }

        fileStorageService.deleteFile(fileId);
    }

    // ==========================================
    // GET FILES FOR COMMENT
    // ==========================================
    public List<FileResponse> getFilesForComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (comment.isDeleted()) {
            throw new ResourceNotFoundException("Comment has been deleted");
        }

        List<File> files = fileRepository.findFilesByCommentIdOrderByUploadedAtAsc(commentId);
        return files.stream()
                .map(this::mapToFileResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // CONVERTERS
    // ==========================================

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getUserId());
        response.setUserName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setDeleted(comment.isDeleted());

        // Count active replies
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            long replyCount = comment.getReplies().stream()
                    .filter(reply -> !reply.isDeleted())
                    .count();
            response.setReplyCount((int) replyCount);
        } else {
            response.setReplyCount(0);
        }

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getCommentId());
            response.setParentCommentContent(comment.getParentComment().getContent());
        }

        if (comment.getTask() != null) {
            response.setTaskId(comment.getTask().getTaskId());
            response.setTaskTitle(comment.getTask().getTitle());
            if (comment.getTask().getTeam() != null) {
                response.setTeamId(comment.getTask().getTeam().getTeamId());
                response.setTeamName(comment.getTask().getTeam().getName());
            }
        } else {
            Project project = comment.getProject();
            if (project != null && comment.getUser() != null) {
                Team team = projectAccessService.findUserTeamOnProject(
                        project.getProjectId(), comment.getUser().getUserId());
                if (team != null) {
                    response.setTeamId(team.getTeamId());
                    response.setTeamName(team.getName());
                }
            }
        }

        // Get files for this comment
        List<File> files = fileRepository.findFilesByCommentIdOrderByUploadedAtAsc(comment.getCommentId());
        response.setFiles(files.stream()
                .map(this::mapToFileResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private FileResponse mapToFileResponse(File file) {
        FileResponse response = new FileResponse();
        response.setFileId(file.getFileId());
        response.setFileName(file.getFileName());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getFileType());
        response.setFileUrl("/api/files/download/" + file.getFileId());
        response.setUploadedAt(file.getUploadedAt());
        response.setUploadedBy(file.getUploadedBy().getUserId());
        response.setUploadedByName(file.getUploadedBy().getFirstName() + " " + file.getUploadedBy().getLastName());
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
}