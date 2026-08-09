package com.example.Project_Management.repository;

import com.example.Project_Management.model.Comment;
import com.example.Project_Management.model.TaskCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskOrderByCreatedAtAsc(TaskCard task); // oldest comment first, like a real thread
}