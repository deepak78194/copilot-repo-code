package com.example.user.model;

import java.time.Instant;

/**
 * Response DTO for a user account. Returned by all user endpoints.
 * Never expose the {@link User} JPA entity directly from the controller layer.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    /** Factory method — maps a {@link User} entity to this DTO. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
