package com.example.Project_Management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long taskId;
    private Long authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}