package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionReplyResponse {
    private Long replyId;
    private String content;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}