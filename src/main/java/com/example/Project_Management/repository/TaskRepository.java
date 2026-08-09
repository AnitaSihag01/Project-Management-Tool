package com.example.Project_Management.repository;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.TaskCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskCard, Long> {
    List<TaskCard> findByProject(Project project); // tasks belonging to one board
}