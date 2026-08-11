package com.example.Project_Management.service;

import com.example.Project_Management.dto.CommentRequest;
import com.example.Project_Management.dto.CommentResponse;
import com.example.Project_Management.exception.ApiException;
import com.example.Project_Management.model.Comment;
import com.example.Project_Management.model.TaskCard;
import com.example.Project_Management.model.User;
import com.example.Project_Management.repository.CommentRepository;
import com.example.Project_Management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final SimpMessagingTemplate messagingTemplate;

    private void broadcastBoardUpdate(Long projectId) {
        messagingTemplate.convertAndSend("/topic/projects/" + projectId, "BOARD_UPDATED");
    }

    public CommentResponse addComment(Long taskId, CommentRequest request, User currentUser) {
        TaskCard task = getTaskOrThrow(taskId);
        projectService.assertIsMember(task.getProject(), currentUser); // must be in the project to comment

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(currentUser);
        comment.setContent(request.getContent());
        commentRepository.save(comment);
        messagingTemplate.convertAndSend("/topic/projects/" + task.getProject().getId(), "BOARD_UPDATED");
        return toResponse(comment);
    }

    public List<CommentResponse> getCommentsForTask(Long taskId, User currentUser) {
        TaskCard task = getTaskOrThrow(taskId);
        projectService.assertIsMember(task.getProject(), currentUser);

        return commentRepository.findByTaskOrderByCreatedAtAsc(task).stream()
                .map(this::toResponse)
                .toList();
    }

    private TaskCard getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId(), c.getTask().getId(), c.getAuthor().getId(),
                c.getAuthor().getFullName(), c.getContent(), c.getCreatedAt()
        );
    }
}