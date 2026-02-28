package com.example.user.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating a user account (PATCH /api/v1/users/{id}).
 */
public record UpdateUserRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name
) {}
