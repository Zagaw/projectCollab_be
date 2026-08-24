package com.example.projectCollab.repository;

import com.example.projectCollab.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFile_FileIdOrderByVersionNumberDesc(Long fileId);

    @Query("SELECT MAX(fv.versionNumber) FROM FileVersion fv WHERE fv.file.fileId = :fileId")
    Optional<Integer> findMaxVersionNumber(@Param("fileId") Long fileId);
}