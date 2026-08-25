package com.example.projectCollab.service;

import com.example.projectCollab.dto.FileVersionResponse;
import com.example.projectCollab.entity.File;
import com.example.projectCollab.entity.FileVersion;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.exception.FileStorageException;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.repository.FileRepository;
import com.example.projectCollab.repository.FileVersionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileVersionService {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    private final FileHelperService fileHelperService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    public FileVersionService(FileVersionRepository fileVersionRepository,
                              FileRepository fileRepository,
                              FileHelperService fileHelperService) {
        this.fileVersionRepository = fileVersionRepository;
        this.fileRepository = fileRepository;
        this.fileHelperService = fileHelperService;
    }

    // ==========================================
    // CREATE NEW FILE VERSION - RETURNS DTO
    // ==========================================
    @Transactional
    public FileVersionResponse createNewVersion(Long fileId, MultipartFile newFile, User uploadedBy, String changeComment) throws IOException {
        File file = fileHelperService.getFileById(fileId);

        // Get the latest version number
        Integer maxVersion = fileVersionRepository.findMaxVersionNumber(fileId).orElse(0);
        int newVersionNumber = maxVersion + 1;

        // Create versions directory
        Path versionDir = Paths.get(uploadDir, "versions", fileId.toString()).toAbsolutePath().normalize();
        if (!Files.exists(versionDir)) {
            Files.createDirectories(versionDir);
        }

        // Store new version file
        String originalFileName = newFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String versionFileName = "v" + newVersionNumber + "_" + UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = versionDir.resolve(versionFileName);
        Files.copy(newFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create version entity
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(newVersionNumber);
        version.setFileName(originalFileName != null ? originalFileName : "unnamed");
        version.setFileSize(newFile.getSize());
        version.setFilePath(targetLocation.toString());
        version.setUploadedBy(uploadedBy);
        version.setChangeComment(changeComment);
        version.setCreatedAt(LocalDateTime.now());

        FileVersion savedVersion = fileVersionRepository.save(version);

        // Update file's current version
        file.setCurrentVersion(newVersionNumber);
        fileRepository.save(file);

        // Return DTO instead of entity
        return mapToResponse(savedVersion);
    }

    // ==========================================
    // CREATE INITIAL VERSION
    // ==========================================
    @Transactional
    public FileVersion createInitialVersion(File file, MultipartFile uploadedFile, User uploadedBy) throws IOException {
        // Create versions directory
        Path versionDir = Paths.get(uploadDir, "versions", file.getFileId().toString()).toAbsolutePath().normalize();
        if (!Files.exists(versionDir)) {
            Files.createDirectories(versionDir);
        }

        // Store initial version
        String originalFileName = uploadedFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String versionFileName = "v1_" + UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = versionDir.resolve(versionFileName);
        Files.copy(uploadedFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create version entity
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(1);
        version.setFileName(originalFileName != null ? originalFileName : "unnamed");
        version.setFileSize(uploadedFile.getSize());
        version.setFilePath(targetLocation.toString());
        version.setUploadedBy(uploadedBy);
        version.setChangeComment("Initial upload");
        version.setCreatedAt(LocalDateTime.now());

        return fileVersionRepository.save(version);
    }

    // ==========================================
    // GET ALL VERSIONS - RETURNS DTOS
    // ==========================================
    public List<FileVersionResponse> getVersionsForFile(Long fileId) {
        fileHelperService.getFileById(fileId);
        List<FileVersion> versions = fileVersionRepository.findByFile_FileIdOrderByVersionNumberDesc(fileId);
        return versions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // GET SPECIFIC VERSION - RETURNS DTO
    // ==========================================
    public FileVersionResponse getVersionResponse(Long fileId, Integer versionNumber) {
        FileVersion version = fileVersionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for file " + fileId));
        return mapToResponse(version);
    }

    // ==========================================
    // GET SPECIFIC VERSION (Entity) - For internal use
    // ==========================================
    public FileVersion getVersion(Long fileId, Integer versionNumber) {
        return fileVersionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for file " + fileId));
    }

    // ==========================================
    // GET LATEST VERSION - RETURNS DTO
    // ==========================================
    public FileVersionResponse getLatestVersionResponse(Long fileId) {
        FileVersion version = fileVersionRepository.findLatestVersionByFileId(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("No versions found for file: " + fileId));
        return mapToResponse(version);
    }

    // ==========================================
    // LOAD VERSION AS RESOURCE
    // ==========================================
    public Resource loadVersionAsResource(Long fileId, Integer versionNumber) throws IOException {
        FileVersion version = getVersion(fileId, versionNumber);
        Path filePath = Paths.get(version.getFilePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileStorageException("Version file not found at path: " + filePath);
        }
    }

    // ==========================================
    // ROLLBACK TO VERSION - RETURNS DTO
    // ==========================================
    @Transactional
    public FileVersionResponse rollbackToVersion(Long fileId, Integer versionNumber, User rolledBackBy, String reason) throws IOException {
        FileVersion targetVersion = getVersion(fileId, versionNumber);
        File file = fileHelperService.getFileById(fileId);

        // Get the latest version number
        Integer maxVersion = fileVersionRepository.findMaxVersionNumber(fileId).orElse(0);
        int newVersionNumber = maxVersion + 1;

        // Copy the target version's file as new version
        Path sourcePath = Paths.get(targetVersion.getFilePath()).toAbsolutePath().normalize();
        Path versionsDir = Paths.get(uploadDir, "versions", fileId.toString()).toAbsolutePath().normalize();

        String originalFileName = targetVersion.getFileName();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String versionFileName = "v" + newVersionNumber + "_" + UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = versionsDir.resolve(versionFileName);
        Files.copy(sourcePath, targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Create new version entity
        FileVersion newVersion = new FileVersion();
        newVersion.setFile(file);
        newVersion.setVersionNumber(newVersionNumber);
        newVersion.setFileName(targetVersion.getFileName());
        newVersion.setFileSize(targetVersion.getFileSize());
        newVersion.setFilePath(targetLocation.toString());
        newVersion.setUploadedBy(rolledBackBy);
        newVersion.setChangeComment("Rollback to version " + versionNumber + (reason != null ? ": " + reason : ""));
        newVersion.setCreatedAt(LocalDateTime.now());

        FileVersion savedVersion = fileVersionRepository.save(newVersion);

        // Update file's current version
        file.setCurrentVersion(newVersionNumber);
        fileRepository.save(file);

        return mapToResponse(savedVersion);
    }

    // ==========================================
    // GET VERSION COUNT
    // ==========================================
    public long getVersionCount(Long fileId) {
        return fileVersionRepository.countVersionsByFileId(fileId);
    }

    // ==========================================
    // DELETE SPECIFIC VERSION
    // ==========================================
    @Transactional
    public void deleteVersion(Long fileId, Integer versionNumber) throws IOException {
        FileVersion version = getVersion(fileId, versionNumber);
        File file = fileHelperService.getFileById(fileId);

        // Don't allow deleting the only version
        if (fileVersionRepository.countVersionsByFileId(fileId) <= 1) {
            throw new IllegalStateException("Cannot delete the only version of a file");
        }

        // Delete physical file
        Path filePath = Paths.get(version.getFilePath()).toAbsolutePath().normalize();
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        fileVersionRepository.delete(version);

        // Update current version if needed
        if (file.getCurrentVersion().equals(versionNumber)) {
            FileVersion latest = fileVersionRepository.findLatestVersionByFileId(fileId).orElse(null);
            if (latest != null) {
                file.setCurrentVersion(latest.getVersionNumber());
                fileRepository.save(file);
            }
        }
    }

    // ==========================================
    // DELETE ALL VERSIONS (when file is deleted)
    // ==========================================
    @Transactional
    public void deleteAllVersions(Long fileId) throws IOException {
        List<FileVersion> versions = fileVersionRepository.findByFile_FileIdOrderByVersionNumberDesc(fileId);

        // Delete physical files
        for (FileVersion version : versions) {
            Path filePath = Paths.get(version.getFilePath()).toAbsolutePath().normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }

        // Delete from database
        fileVersionRepository.deleteAll(versions);
    }

    // ==========================================
    // COMPARE TWO VERSIONS
    // ==========================================
    public VersionComparison compareVersions(Long fileId, Integer version1, Integer version2) {
        FileVersion v1 = getVersion(fileId, version1);
        FileVersion v2 = getVersion(fileId, version2);

        VersionComparison comparison = new VersionComparison();
        comparison.setVersion1Number(v1.getVersionNumber());
        comparison.setVersion1Name(v1.getFileName());
        comparison.setVersion1Size(v1.getFileSize());
        comparison.setVersion1UploadedAt(v1.getCreatedAt());
        comparison.setVersion2Number(v2.getVersionNumber());
        comparison.setVersion2Name(v2.getFileName());
        comparison.setVersion2Size(v2.getFileSize());
        comparison.setVersion2UploadedAt(v2.getCreatedAt());

        long sizeDiff = v2.getFileSize() - v1.getFileSize();
        comparison.setSizeDifference(sizeDiff);

        if (v1.getFileSize() > 0) {
            comparison.setSizeDifferencePercent((double) sizeDiff / v1.getFileSize() * 100);
        } else {
            comparison.setSizeDifferencePercent(0.0);
        }

        return comparison;
    }

    // ==========================================
    // MAP TO DTO - BREAKS THE CIRCULAR REFERENCE
    // ==========================================
    private FileVersionResponse mapToResponse(FileVersion version) {
        FileVersionResponse response = new FileVersionResponse();
        response.setVersionId(version.getVersionId());
        response.setVersionNumber(version.getVersionNumber());
        response.setFileName(version.getFileName());
        response.setFileSize(version.getFileSize());
        response.setFilePath(version.getFilePath());
        response.setCreatedAt(version.getCreatedAt());
        response.setChangeComment(version.getChangeComment());

        if (version.getFile() != null) {
            response.setFileId(version.getFile().getFileId());
            response.setOriginalFileName(version.getFile().getFileName());
            response.setIsCurrentVersion(version.getFile().getCurrentVersion().equals(version.getVersionNumber()));
            response.setDownloadUrl("/api/files/" + version.getFile().getFileId() + "/versions/" + version.getVersionNumber() + "/download");
        }

        if (version.getUploadedBy() != null) {
            response.setUploadedBy(version.getUploadedBy().getUserId());
            response.setUploadedByName(version.getUploadedBy().getFirstName() + " " +
                    version.getUploadedBy().getLastName());
        }

        return response;
    }

    // ==========================================
    // INNER CLASS: Version Comparison
    // ==========================================
    public static class VersionComparison {
        private Integer version1Number;
        private String version1Name;
        private Long version1Size;
        private LocalDateTime version1UploadedAt;
        private Integer version2Number;
        private String version2Name;
        private Long version2Size;
        private LocalDateTime version2UploadedAt;
        private Long sizeDifference;
        private Double sizeDifferencePercent;

        // Getters and Setters
        public Integer getVersion1Number() { return version1Number; }
        public void setVersion1Number(Integer version1Number) { this.version1Number = version1Number; }
        public String getVersion1Name() { return version1Name; }
        public void setVersion1Name(String version1Name) { this.version1Name = version1Name; }
        public Long getVersion1Size() { return version1Size; }
        public void setVersion1Size(Long version1Size) { this.version1Size = version1Size; }
        public LocalDateTime getVersion1UploadedAt() { return version1UploadedAt; }
        public void setVersion1UploadedAt(LocalDateTime version1UploadedAt) { this.version1UploadedAt = version1UploadedAt; }
        public Integer getVersion2Number() { return version2Number; }
        public void setVersion2Number(Integer version2Number) { this.version2Number = version2Number; }
        public String getVersion2Name() { return version2Name; }
        public void setVersion2Name(String version2Name) { this.version2Name = version2Name; }
        public Long getVersion2Size() { return version2Size; }
        public void setVersion2Size(Long version2Size) { this.version2Size = version2Size; }
        public LocalDateTime getVersion2UploadedAt() { return version2UploadedAt; }
        public void setVersion2UploadedAt(LocalDateTime version2UploadedAt) { this.version2UploadedAt = version2UploadedAt; }
        public Long getSizeDifference() { return sizeDifference; }
        public void setSizeDifference(Long sizeDifference) { this.sizeDifference = sizeDifference; }
        public Double getSizeDifferencePercent() { return sizeDifferencePercent; }
        public void setSizeDifferencePercent(Double sizeDifferencePercent) { this.sizeDifferencePercent = sizeDifferencePercent; }
    }
}