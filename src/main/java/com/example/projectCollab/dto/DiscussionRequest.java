package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRequest {

    @NotBlank(message = "Discussion title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    @NotBlank(message = "Discussion content is required")
    @Size(max = 2000, message = "Content must be less than 2000 characters")
    private String content;
}