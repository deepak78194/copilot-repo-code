package com.example.user.service;

import com.example.user.exception.DuplicateEmailException;
import com.example.user.exception.UserNotFoundException;
import com.example.user.model.CreateUserRequest;
import com.example.user.model.UpdateUserRequest;
import com.example.user.model.User;
import com.example.user.model.UserResponse;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final String VALID_EMAIL = "alice@example.com";
    private static final String VALID_NAME  = "Alice";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ── createUser ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser: returns saved user response when input is valid")
    void createUser_withValidInput_returnsSavedUserResponse() {
        var request = new CreateUserRequest(VALID_EMAIL, VALID_NAME);
        var savedUser = new User(VALID_EMAIL, VALID_NAME);

        when(userRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertThat(response.email()).isEqualTo(VALID_EMAIL);
        assertThat(response.name()).isEqualTo(VALID_NAME);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser: throws DuplicateEmailException when email is already taken")
    void createUser_withDuplicateEmail_throwsDuplicateEmailException() {
        var request = new CreateUserRequest(VALID_EMAIL, VALID_NAME);

        when(userRepository.existsByEmail(VALID_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(VALID_EMAIL);

        verify(userRepository, never()).save(any());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns user response when user exists")
    void findById_withExistingId_returnsUserResponse() {
        var user = new User(VALID_EMAIL, VALID_NAME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.email()).isEqualTo(VALID_EMAIL);
    }

    @Test
    @DisplayName("findById: throws UserNotFoundException when user does not exist")
    void findById_withNonExistentId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser: updates name and returns updated response")
    void updateUser_withValidInput_returnsUpdatedResponse() {
        var user = new User(VALID_EMAIL, VALID_NAME);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUser(1L, new UpdateUserRequest("Bob"));

        assertThat(response.name()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("updateUser: throws UserNotFoundException when user does not exist")
    void updateUser_withNonExistentId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest("Bob")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser: deletes user when user exists")
    void deleteUser_withExistingId_deletesUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser: throws UserNotFoundException when user does not exist")
    void deleteUser_withNonExistentId_throwsUserNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
