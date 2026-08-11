package com.example.Project_Management.dto;

import com.example.Project_Management.model.type.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private TaskStatus status; // TODO, IN_PROGRESS, or DONE
}