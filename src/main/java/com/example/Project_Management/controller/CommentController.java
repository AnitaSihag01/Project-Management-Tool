package com.example.Project_Management.controller;

import com.example.Project_Management.dto.CommentRequest;
import com.example.Project_Management.dto.CommentResponse;
import com.example.Project_Management.model.User;
import com.example.Project_Management.security.CurrentUserProvider;
import com.example.Project_Management.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(commentService.addComment(taskId, request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long taskId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(commentService.getCommentsForTask(taskId, currentUser));
    }
}