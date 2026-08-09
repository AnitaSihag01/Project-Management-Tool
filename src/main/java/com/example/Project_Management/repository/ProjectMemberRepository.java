package com.example.Project_Management.repository;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.ProjectMember;
import com.example.Project_Management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProject(Project project);   // all members of a project
    List<ProjectMember> findByUser(User user);             // all projects a user belongs to
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    boolean existsByProjectAndUser(Project project, User user); // quick membership check
}