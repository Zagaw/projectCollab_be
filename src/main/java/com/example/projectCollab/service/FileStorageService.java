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

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    public FileStorageService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Transactional
    public File storeFile(MultipartFile file, Long commentId, User uploadedBy) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Store file
        Path targetLocation = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create file entity
        File fileEntity = new File();
        fileEntity.setFileName(originalFileName != null ? originalFileName : "unnamed");
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        fileEntity.setFilePath(targetLocation.toString());
        fileEntity.setStorageType(File.StorageType.LOCAL);
        fileEntity.setUploadedBy(uploadedBy);

        return fileEntity;
    }

    public Resource loadFileAsResource(Long fileId) throws IOException {
        File file = getFileById(fileId);
        Path filePath = Paths.get(file.getFilePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileStorageException("File not found at path: " + filePath);
        }
    }

    // ✅ ADD THIS METHOD
    public File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        File file = getFileById(fileId);

        // Delete physical file
        Path filePath = Paths.get(file.getFilePath()).toAbsolutePath().normalize();
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        fileRepository.delete(file);
    }

    @Transactional
    public File updateFile(Long fileId, MultipartFile newFile, User uploadedBy) throws IOException {
        File existingFile = getFileById(fileId);

        // Delete old file
        Path oldPath = Paths.get(existingFile.getFilePath()).toAbsolutePath().normalize();
        if (Files.exists(oldPath)) {
            Files.delete(oldPath);
        }

        // Store new file
        String originalFileName = newFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path targetLocation = uploadPath.resolve(uniqueFileName);
        Files.copy(newFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Update entity
        existingFile.setFileName(originalFileName != null ? originalFileName : "unnamed");
        existingFile.setFileSize(newFile.getSize());
        existingFile.setFileType(newFile.getContentType() != null ? newFile.getContentType() : "application/octet-stream");
        existingFile.setFilePath(targetLocation.toString());

        return fileRepository.save(existingFile);
    }

    public FileUploadResponse getFileUploadResponse(File file) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(file.getFileId());
        response.setFileName(file.getFileName());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getFileType());
        response.setDownloadUrl("/api/files/download/" + file.getFileId());
        response.setMessage("File uploaded successfully");
        return response;
    }
}