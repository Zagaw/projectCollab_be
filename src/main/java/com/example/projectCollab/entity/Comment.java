package com.example.projectCollab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete flag
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    // Replies to this comment
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    // ✅ ADDED: One-to-many relationship with files
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<File> files = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isDeleted() {
        return isDeleted != null && isDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Comment getParentComment() {
        return parentComment;
    }

    public void setParentComment(Comment parentComment) {
        this.parentComment = parentComment;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    // ✅ ADDED: Getter and Setter for files
    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Add a file to this comment
     */
    public void addFile(File file) {
        files.add(file);
        file.setComment(this);
    }

    /**
     * Remove a file from this comment
     */
    public void removeFile(File file) {
        files.remove(file);
        file.setComment(null);
    }

    /**
     * Check if comment has any files
     */
    public boolean hasFiles() {
        return files != null && !files.isEmpty();
    }

    /**
     * Get total size of all files in this comment
     */
    public long getTotalFileSize() {
        if (files == null || files.isEmpty()) {
            return 0;
        }
        return files.stream()
                .mapToLong(File::getFileSize)
                .sum();
    }

    /**
     * Add a reply to this comment
     */
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }

    /**
     * Remove a reply from this comment
     */
    public void removeReply(Comment reply) {
        replies.remove(reply);
        reply.setParentComment(null);
    }

    /**
     * Check if comment has any replies
     */
    public boolean hasReplies() {
        return replies != null && !replies.isEmpty();
    }

    /**
     * Get number of replies
     */
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }

    /**
     * Check if comment is a reply (has parent)
     */
    public boolean isReply() {
        return parentComment != null;
    }
}