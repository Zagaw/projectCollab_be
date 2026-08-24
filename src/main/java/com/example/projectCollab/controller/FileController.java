package com.example.projectCollab.controller;

import com.example.projectCollab.dto.FileUploadResponse;
import com.example.projectCollab.entity.File;
import com.example.projectCollab.service.FileStorageService;
import com.example.projectCollab.util.AuthUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthUtil authUtil;

    public FileController(FileStorageService fileStorageService, AuthUtil authUtil) {
        this.fileStorageService = fileStorageService;
        this.authUtil = authUtil;
    }

    // ==========================================
    // DOWNLOAD FILE
    // ==========================================
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        
        // Get file entity to get the filename
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
    // GET FILE INFO
    // ==========================================
    @GetMapping("/{fileId}/info")
    public ResponseEntity<FileUploadResponse> getFileInfo(@PathVariable Long fileId) {
        File file = fileStorageService.getFileById(fileId);
        return ResponseEntity.ok(fileStorageService.getFileUploadResponse(file));
    }

    // ==========================================
    // STREAM FILE (for preview)
    // ==========================================
    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(fileId);
        File file = fileStorageService.getFileById(fileId);
        
        String contentType = file.getFileType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // For images and PDFs, display inline; for others, download
        String contentDisposition = "attachment";
        if (contentType.startsWith("image/") || contentType.equals("application/pdf")) {
            contentDisposition = "inline";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + "; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }
}