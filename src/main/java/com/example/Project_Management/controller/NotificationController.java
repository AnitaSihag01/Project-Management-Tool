package com.example.Project_Management.controller;

import com.example.Project_Management.dto.NotificationResponse;
import com.example.Project_Management.model.User;
import com.example.Project_Management.security.CurrentUserProvider;
import com.example.Project_Management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        User currentUser = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.getNotificationsFor(currentUser));
    }
}