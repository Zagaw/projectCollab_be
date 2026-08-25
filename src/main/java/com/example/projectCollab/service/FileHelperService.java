package com.example.projectCollab.service;

import com.example.projectCollab.entity.File;
import com.example.projectCollab.exception.ResourceNotFoundException;
import com.example.projectCollab.repository.FileRepository;
import org.springframework.stereotype.Service;

@Service
public class FileHelperService {

    private final FileRepository fileRepository;

    public FileHelperService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public File getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
    }
}