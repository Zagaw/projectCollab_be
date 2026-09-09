package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private Long uploadedBy;
    private String uploadedByName;
    private String storageType;
    private Integer versionNumber;
    private String category;
    private Long projectId;
    private Long teamId;
    private String teamName;
    private Long commentId;
}