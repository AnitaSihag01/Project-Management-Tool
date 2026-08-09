package com.example.Project_Management.controller;

import com.example.Project_Management.dto.*;
import com.example.Project_Management.model.User;
import com.example.Project_Management.security.CurrentUserProvider;
import com.example.Project_Management.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(projectService.createProject(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects() {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(projectService.getProjectsForUser(currentUser));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(projectService.addMember(projectId, request, currentUser));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable Long projectId) {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(projectService.getMembers(projectId, currentUser));
    }
}