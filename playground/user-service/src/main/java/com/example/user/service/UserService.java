package com.example.user.service;

import com.example.user.exception.DuplicateEmailException;
import com.example.user.exception.UserNotFoundException;
import com.example.user.model.CreateUserRequest;
import com.example.user.model.UpdateUserRequest;
import com.example.user.model.User;
import com.example.user.model.UserResponse;
import com.example.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user account management.
 * All public methods map domain operations to repository calls and enforce invariants.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns a paginated list of all users.
     *
     * @param pageable pagination parameters
     * @return a page of {@link UserResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    /**
     * Returns the user with the given ID.
     *
     * @param id the user's primary key
     * @return the {@link UserResponse} DTO
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Creates a new user account.
     *
     * @param request the creation request containing email and name
     * @return the created {@link UserResponse} DTO
     * @throws DuplicateEmailException if the email address is already taken
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        var user = new User(request.email(), request.name());
        var saved = userRepository.save(user);
        log.info("User created userId={}", saved.getId());
        return UserResponse.from(saved);
    }

    /**
     * Updates the name of an existing user.
     *
     * @param id      the user's primary key
     * @param request the update request containing the new name
     * @return the updated {@link UserResponse} DTO
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setName(request.name());
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Deletes the user with the given ID.
     *
     * @param id the user's primary key
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("User deleted userId={}", id);
    }
}
