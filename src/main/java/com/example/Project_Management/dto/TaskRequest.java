package com.example.Project_Management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskRequest {
    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    // Optional: email of the person to assign this task to at creation time
    private String assigneeEmail;
}