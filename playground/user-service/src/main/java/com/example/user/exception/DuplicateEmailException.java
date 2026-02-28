package com.example.user.exception;

/**
 * Thrown when a user with the given email address already exists. Maps to HTTP 409.
 */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("A user with email " + email + " already exists.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
