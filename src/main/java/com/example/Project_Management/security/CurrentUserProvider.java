package com.example.Project_Management.security;

import com.example.Project_Management.exception.ApiException;
import com.example.Project_Management.model.User;
import com.example.Project_Management.repository.UserRepository;
import com.example.Project_Management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // this is the email, since that's what we set as the "username" in CustomUserDetailsService

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Logged-in user not found", HttpStatus.UNAUTHORIZED));
    }
}