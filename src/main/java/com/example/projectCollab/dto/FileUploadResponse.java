package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String downloadUrl;
    private String message;
}