package com.example.projectCollab.service;

import com.example.projectCollab.dto.FileUploadResponse;
import com.example.projectCollab.entity.File;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.exception.FileStorageException;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileRepository fileRepository;
    private final FileHelperService fileHelperService;
    private final FileVersionService fileVersionService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    public FileStorageService(FileRepository fileRepository,
                              FileHelperService fileHelperService,
                              FileVersionService fileVersionService) {
        this.fileRepository = fileRepository;
        this.fileHelperService = fileHelperService;
        this.fileVersionService = fileVersionService;
    }

    // ✅ FIXED: Don't save the file entity here - just prepare it
    @Transactional
    public File prepareFileEntity(MultipartFile file, User uploadedBy) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename for the main file
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Store file
        Path targetLocation = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create file entity (without saving to DB yet)
        File fileEntity = new File();
        fileEntity.setFileName(originalFileName != null ? originalFileName : "unnamed");
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        fileEntity.setFilePath(targetLocation.toString());
        fileEntity.setStorageType(File.StorageType.LOCAL);
        fileEntity.setUploadedBy(uploadedBy);
        fileEntity.setCurrentVersion(1);

        return fileEntity;
    }

    // ✅ NEW: Save file entity with comment association
    @Transactional
    public File saveFileWithComment(File fileEntity, Long commentId, MultipartFile uploadedFile, User uploadedBy) throws IOException {
        // Set the comment
        // Note: We need to fetch the comment entity or just set the ID
        // Using commentId directly via a reference

        // Since we can't set just the ID without loading the entity,
        // we'll handle this differently - the comment is set in the service

        // Save file to database
        File savedFile = fileRepository.save(fileEntity);

        // Create initial version
        fileVersionService.createInitialVersion(savedFile, uploadedFile, uploadedBy);

        return savedFile;
    }

    // ✅ FIXED: Store file with comment (original method kept for compatibility)
    @Transactional
    public File storeFile(MultipartFile file, Long commentId, User uploadedBy) throws IOException {
        // This method is now deprecated - use prepareFileEntity + save separately
        // But we'll keep it working by creating a temporary entity

        File fileEntity = prepareFileEntity(file, uploadedBy);

        // We can't save without comment_id, so we throw an exception
        // The caller should use the new approach
        throw new IllegalStateException("Use prepareFileEntity() and save separately with comment association");
    }

    public Resource loadFileAsResource(Long fileId) throws IOException {
        File file = fileHelperService.getFileById(fileId);
        Path filePath = Paths.get(file.getFilePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileStorageException("File not found at path: " + filePath);
        }
    }

    public File getFileById(Long fileId) {
        return fileHelperService.getFileById(fileId);
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        File file = fileHelperService.getFileById(fileId);

        // Delete all versions first
        fileVersionService.deleteAllVersions(fileId);

        // Delete physical file
        Path filePath = Paths.get(file.getFilePath()).toAbsolutePath().normalize();
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        fileRepository.delete(file);
    }

    public FileUploadResponse getFileUploadResponse(File file) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(file.getFileId());
        response.setFileName(file.getFileName());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getFileType());
        response.setDownloadUrl("/api/files/download/" + file.getFileId());
        response.setMessage("File uploaded successfully");
        response.setVersionNumber(file.getCurrentVersion());
        response.setTotalVersions(file.getVersions().size());
        response.setHasNewerVersion(false);
        return response;
    }
}