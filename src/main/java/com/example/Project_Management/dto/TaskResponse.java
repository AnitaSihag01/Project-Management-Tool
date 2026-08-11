package com.example.Project_Management.dto;

import com.example.Project_Management.model.type.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assigneeId;
    private String assigneeName; // null if unassigned
    private String createdByName;
    private LocalDateTime createdAt;
}