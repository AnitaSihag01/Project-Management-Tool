package com.example.Project_Management.service;

import com.example.Project_Management.dto.*;
import com.example.Project_Management.exception.ApiException;
import com.example.Project_Management.model.*;
import com.example.Project_Management.model.type.ProjectRole;
import com.example.Project_Management.repository.ProjectMemberRepository;
import com.example.Project_Management.repository.ProjectRepository;
import com.example.Project_Management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;


    public ProjectResponse createProject(ProjectRequest request, User currentUser) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(currentUser);
        projectRepository.save(project);

        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(project);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(ProjectRole.OWNER);
        memberRepository.save(ownerMembership);

        return toResponse(project);
    }


    public List<ProjectResponse> getProjectsForUser(User user) {
        return memberRepository.findByUser(user).stream()
                .map(ProjectMember::getProject)
                .map(this::toResponse)
                .toList();
    }

    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
    }


    public void assertIsMember(Project project, User user) {
        if (!memberRepository.existsByProjectAndUser(project, user)) {
            throw new ApiException("You are not a member of this project", HttpStatus.FORBIDDEN);
        }
    }


    public void assertIsOwner(Project project, User user) {
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new ApiException("Only the project owner can do this", HttpStatus.FORBIDDEN);
        }
    }

    public MemberResponse addMember(Long projectId, AddMemberRequest request, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertIsOwner(project, currentUser);

        User newMember = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("No user found with that email", HttpStatus.NOT_FOUND));

        if (memberRepository.existsByProjectAndUser(project, newMember)) {
            throw new ApiException("That user is already a member of this project", HttpStatus.CONFLICT);
        }

        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(newMember);
        membership.setRole(ProjectRole.MEMBER);
        memberRepository.save(membership);

        return toMemberResponse(membership);
    }

    public List<MemberResponse> getMembers(Long projectId, User currentUser) {
        Project project = getProjectOrThrow(projectId);
        assertIsMember(project, currentUser);

        return memberRepository.findByProject(project).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getOwner().getId(), p.getOwner().getFullName(), p.getCreatedAt()
        );
    }

    private MemberResponse toMemberResponse(ProjectMember m) {
        return new MemberResponse(
                m.getUser().getId(), m.getUser().getFullName(),
                m.getUser().getEmail(), m.getRole().name()
        );
    }
}