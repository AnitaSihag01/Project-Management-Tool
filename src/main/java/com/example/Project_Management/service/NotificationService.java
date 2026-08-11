package com.example.Project_Management.service;

import com.example.Project_Management.dto.NotificationResponse;
import com.example.Project_Management.model.Notification;
import com.example.Project_Management.model.User;
import com.example.Project_Management.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** Saves a notification AND pushes it live if the user is currently connected. */
    public void notifyUser(User recipient, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notificationRepository.save(notification);

        NotificationResponse payload = toResponse(notification);

        // Spring routes this to whichever open WebSocket session belongs to this user's email
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                payload
        );
    }

    public List<NotificationResponse> getNotificationsFor(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}