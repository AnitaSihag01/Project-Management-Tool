package com.example.Project_Management.controller;

import com.example.Project_Management.dto.TaskRequest;
import com.example.Project_Management.dto.TaskResponse;
import com.example.Project_Management.dto.TaskStatusUpdateRequest;
import com.example.Project_Management.model.User;
import com.example.Project_Management.security.CurrentUserProvider;
import com.example.Project_Management.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.createTask(projectId, request, currentUser));
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable Long projectId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.getTasksForProject(projectId, currentUser));
    }

    @PutMapping("/api/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.updateStatus(taskId, request, currentUser));
    }

    @PutMapping("/api/tasks/{taskId}/assignee")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(taskService.assignTask(taskId, body.get("email"), currentUser));
    }
}