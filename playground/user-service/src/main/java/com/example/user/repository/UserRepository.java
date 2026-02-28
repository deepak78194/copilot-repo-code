package com.example.user.repository;

import com.example.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access interface for {@link User} entities.
 * No business logic belongs here — delegate to {@link com.example.user.service.UserService}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Returns the user with the given email address, or empty if no such user exists.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the matching user, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns {@code true} if a user with the given email address already exists.
     *
     * @param email the email address to check
     * @return {@code true} if the email is already taken
     */
    boolean existsByEmail(String email);
}
