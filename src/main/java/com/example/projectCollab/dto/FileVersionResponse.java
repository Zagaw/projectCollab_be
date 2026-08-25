package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionResponse {
    private Long versionId;
    private Integer versionNumber;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private LocalDateTime createdAt;
    private String changeComment;
    private Long uploadedBy;
    private String uploadedByName;
    private Boolean isCurrentVersion;
    private String downloadUrl;
    private Long fileId;
    private String originalFileName;
}