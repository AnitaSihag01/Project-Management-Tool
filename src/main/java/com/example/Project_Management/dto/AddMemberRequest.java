package com.example.Project_Management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequest {
    @NotBlank
    @Email
    private String email; // the person you want to invite, by their account email
}