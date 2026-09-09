package com.example.projectCollab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FileSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(FileSchemaRepair.class);
    private final JdbcTemplate jdbcTemplate;

    public FileSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void makeCommentOptionalOnFiles() {
        try {
            jdbcTemplate.execute("ALTER TABLE files MODIFY COLUMN comment_id BIGINT NULL");
            log.info("files.comment_id is now nullable so the project file library can store files without a comment");
        } catch (Exception ex) {
            log.warn("Could not relax files.comment_id: {}", ex.getMessage());
        }
    }
}
