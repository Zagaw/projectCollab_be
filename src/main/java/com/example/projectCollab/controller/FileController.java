package com.example.projectCollab.controller;

import com.example.projectCollab.dto.FileUploadResponse;
import com.example.projectCollab.dto.FileVersionResponse;
import com.example.projectCollab.entity.File;
import com.example.projectCollab.entity.FileVersion;
import com.example.projectCollab.entity.User;
import com.example.projectCollab.service.FileStorageService;
import com.example.projectCollab.service.FileVersionService;
import com.example.projectCollab.service.ProjectFileService;
import com.example.projectCollab.util.AuthUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileVersionService fileVersionService;
    private final ProjectFileService projectFileService;
    private final AuthUtil authUtil;

    public FileController(FileStorageService fileStorageService,
                          FileVersionService fileVersionService,
                          ProjectFileService projectFileService,
                          AuthUtil authUtil) {
        this.fileStorageService = fileStorageService;
        this.fileVersionService = fileVersionService;
        this.projectFileService = projectFileService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // DOWNLOAD FILE (Current version)
    // ==========================================

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        File file = fileStorageService.getFileById(fileId);

        String contentType = file.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    // ==========================================
    // VIEW FILE (Preview)
    // ==========================================

    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        File file = fileStorageService.getFileById(fileId);

        String contentType = file.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String contentDisposition = "attachment";
        if (contentType.startsWith("image/") || contentType.equals("application/pdf")) {
            contentDisposition = "inline";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + "; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    // ==========================================
    // GET FILE INFO (with version info)
    // ==========================================

    @GetMapping("/{fileId}/info")
    public ResponseEntity<FileUploadResponse> getFileInfo(@PathVariable Long fileId) {
        File file = fileStorageService.getFileById(fileId);
        FileUploadResponse response = fileStorageService.getFileUploadResponse(file);

        // Add version info
        long versionCount = fileVersionService.getVersionCount(fileId);
        response.setTotalVersions((int) versionCount);
        response.setVersionNumber(file.getCurrentVersion());

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // UPDATE FILE (Create New Version) - Returns DTO
    // ==========================================

    @PostMapping("/{fileId}/versions")
    public ResponseEntity<FileVersionResponse> updateFileVersion(
            @PathVariable Long fileId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeComment", required = false) String changeComment,
            Authentication authentication) throws IOException {

        User currentUser = authUtil.getCurrentUser(authentication);
        FileVersionResponse response = fileVersionService.createNewVersion(fileId, file, currentUser, changeComment);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET ALL VERSIONS FOR A FILE - Returns DTOs
    // ==========================================

    @GetMapping("/{fileId}/versions")
    public ResponseEntity<List<FileVersionResponse>> getFileVersions(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileVersionService.getVersionsForFile(fileId));
    }

    // ==========================================
    // GET SPECIFIC VERSION INFO - Returns DTO
    // ==========================================

    @GetMapping("/{fileId}/versions/{versionNumber}")
    public ResponseEntity<FileVersionResponse> getSpecificVersion(
            @PathVariable Long fileId,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(fileVersionService.getVersionResponse(fileId, versionNumber));
    }

    // ==========================================
    // DOWNLOAD SPECIFIC VERSION
    // ==========================================

    @GetMapping("/{fileId}/versions/{versionNumber}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable Long fileId,
            @PathVariable Integer versionNumber) throws IOException {
        Resource resource = fileVersionService.loadVersionAsResource(fileId, versionNumber);
        FileVersion version = fileVersionService.getVersion(fileId, versionNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"v" + versionNumber + "_" + version.getFileName() + "\"")
                .body(resource);
    }

    // ==========================================
    // VIEW SPECIFIC VERSION (Preview)
    // ==========================================

    @GetMapping("/{fileId}/versions/{versionNumber}/view")
    public ResponseEntity<Resource> viewVersion(
            @PathVariable Long fileId,
            @PathVariable Integer versionNumber) throws IOException {
        Resource resource = fileVersionService.loadVersionAsResource(fileId, versionNumber);
        FileVersion version = fileVersionService.getVersion(fileId, versionNumber);

        String contentType = "application/octet-stream";
        // Try to detect content type from file name
        String fileName = version.getFileName();
        if (fileName != null) {
            if (fileName.endsWith(".pdf")) contentType = "application/pdf";
            else if (fileName.endsWith(".png")) contentType = "image/png";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fileName.endsWith(".gif")) contentType = "image/gif";
            else if (fileName.endsWith(".txt")) contentType = "text/plain";
            else if (fileName.endsWith(".json")) contentType = "application/json";
            else if (fileName.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            else if (fileName.endsWith(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        String contentDisposition = "attachment";
        if (contentType.startsWith("image/") || contentType.equals("application/pdf")) {
            contentDisposition = "inline";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition + "; filename=\"v" + versionNumber + "_" + version.getFileName() + "\"")
                .body(resource);
    }

    // ==========================================
    // ROLLBACK TO VERSION - Returns DTO
    // ==========================================

    @PostMapping("/{fileId}/rollback/{versionNumber}")
    public ResponseEntity<FileVersionResponse> rollbackToVersion(
            @PathVariable Long fileId,
            @PathVariable Integer versionNumber,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication) throws IOException {
        User currentUser = authUtil.getCurrentUser(authentication);
        FileVersionResponse response = fileVersionService.rollbackToVersion(fileId, versionNumber, currentUser, reason);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET LATEST VERSION - Returns DTO
    // ==========================================

    @GetMapping("/{fileId}/versions/latest")
    public ResponseEntity<FileVersionResponse> getLatestVersion(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileVersionService.getLatestVersionResponse(fileId));
    }

    // ==========================================
    // DELETE VERSION
    // ==========================================

    @DeleteMapping("/{fileId}/versions/{versionNumber}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable Long fileId,
            @PathVariable Integer versionNumber) throws IOException {
        fileVersionService.deleteVersion(fileId, versionNumber);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // COMPARE TWO VERSIONS
    // ==========================================

    @GetMapping("/{fileId}/compare")
    public ResponseEntity<FileVersionService.VersionComparison> compareVersions(
            @PathVariable Long fileId,
            @RequestParam Integer v1,
            @RequestParam Integer v2) {
        return ResponseEntity.ok(fileVersionService.compareVersions(fileId, v1, v2));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) throws IOException {
        User currentUser = authUtil.getCurrentUser(authentication);
        projectFileService.deleteFile(fileId, currentUser);
        return ResponseEntity.noContent().build();
    }
}