package com.example.user.exception;

/**
 * Thrown when a requested user does not exist. Maps to HTTP 404.
 */
public class UserNotFoundException extends RuntimeException {

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("No user with id " + userId + " exists.");
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
