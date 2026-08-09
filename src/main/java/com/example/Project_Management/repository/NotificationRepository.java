package com.example.Project_Management.repository;

import com.example.Project_Management.model.Notification;
import com.example.Project_Management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient); // newest notification first
}