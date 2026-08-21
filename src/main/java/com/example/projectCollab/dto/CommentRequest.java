package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 1000, message = "Comment must be less than 1000 characters")
    private String content;

    private Long parentCommentId; // For replies to comments
}