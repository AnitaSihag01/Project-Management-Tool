package com.example.Project_Management.service;

import com.example.Project_Management.dto.TaskRequest;
import com.example.Project_Management.dto.TaskResponse;
import com.example.Project_Management.dto.TaskStatusUpdateRequest;
import com.example.Project_Management.exception.ApiException;
import com.example.Project_Management.model.*;
import com.example.Project_Management.model.type.TaskStatus;
import com.example.Project_Management.repository.TaskRepository;
import com.example.Project_Management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    private void broadcastBoardUpdate(Long projectId) {
        messagingTemplate.convertAndSend("/topic/projects/" + projectId, "BOARD_UPDATED");
    }

    public TaskResponse createTask(Long projectId, TaskRequest request, User currentUser) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectService.assertIsMember(project, currentUser); // must belong to the project to add tasks to it

        TaskCard task = new TaskCard();
        task.setProject(project);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCreatedBy(currentUser);
        task.setStatus(TaskStatus.TODO);

        if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()) {
            User assignee = userRepository.findByEmail(request.getAssigneeEmail())
                    .orElseThrow(() -> new ApiException("No user found with that email", HttpStatus.NOT_FOUND));
            projectService.assertIsMember(project, assignee); // can only assign to people already in the project
            task.setAssignee(assignee);
        }

        taskRepository.save(task);
        if (task.getAssignee() != null) {
            notificationService.notifyUser(task.getAssignee(),
                    currentUser.getFullName() + " assigned you a task: \"" + task.getTitle() + "\"");
        }
        broadcastBoardUpdate(project.getId());
        return toResponse(task);
    }

    public List<TaskResponse> getTasksForProject(Long projectId, User currentUser) {
        Project project = projectService.getProjectOrThrow(projectId);
        projectService.assertIsMember(project, currentUser);

        return taskRepository.findByProject(project).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse updateStatus(Long taskId, TaskStatusUpdateRequest request, User currentUser) {
        TaskCard task = getTaskOrThrow(taskId);
        projectService.assertIsMember(task.getProject(), currentUser);

        task.setStatus(request.getStatus());
        taskRepository.save(task);
        broadcastBoardUpdate(task.getProject().getId());
        return toResponse(task);
    }

    public TaskResponse assignTask(Long taskId, String assigneeEmail, User currentUser) {
        TaskCard task = getTaskOrThrow(taskId);
        Project project = task.getProject();
        projectService.assertIsMember(project, currentUser);

        User assignee = userRepository.findByEmail(assigneeEmail)
                .orElseThrow(() -> new ApiException("No user found with that email", HttpStatus.NOT_FOUND));
        projectService.assertIsMember(project, assignee);

        task.setAssignee(assignee);
        taskRepository.save(task);
        notificationService.notifyUser(assignee,
                currentUser.getFullName() + " assigned you a task: \"" + task.getTitle() + "\"");
        broadcastBoardUpdate(project.getId());
        return toResponse(task);
    }

    private TaskCard getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
    }

    private TaskResponse toResponse(TaskCard t) {
        return new TaskResponse(
                t.getId(),
                t.getProject().getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getFullName() : null,
                t.getCreatedBy().getFullName(),
                t.getCreatedAt()
        );
    }
}