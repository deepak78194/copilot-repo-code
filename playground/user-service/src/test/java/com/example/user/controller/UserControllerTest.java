package com.example.user.controller;

import com.example.user.model.CreateUserRequest;
import com.example.user.model.UpdateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserController} using a real PostgreSQL database via Testcontainers.
 * Each test runs against the full Spring Boot application stack.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserController integration")
class UserControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    // ── POST /api/v1/users ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/users returns 201 with Location header when input is valid")
    void createUser_withValidInput_returns201() {
        var request = new CreateUserRequest("bob@example.com", "Bob");
        var response = rest.postForEntity("/api/v1/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/users returns 400 when email is blank")
    void createUser_withBlankEmail_returns400() {
        var request = new CreateUserRequest("", "Bob");
        var response = rest.postForEntity("/api/v1/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/users returns 400 when email is invalid")
    void createUser_withInvalidEmail_returns400() {
        var request = new CreateUserRequest("not-an-email", "Bob");
        var response = rest.postForEntity("/api/v1/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/users returns 409 when email is already taken")
    void createUser_withDuplicateEmail_returns409() {
        var request = new CreateUserRequest("carol@example.com", "Carol");
        rest.postForEntity("/api/v1/users", request, String.class);

        var duplicate = rest.postForEntity("/api/v1/users", request, String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── GET /api/v1/users/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/users/{id} returns 200 when user exists")
    void getUser_withExistingId_returns200() {
        var created = rest.postForEntity(
                "/api/v1/users", new CreateUserRequest("dave@example.com", "Dave"), String.class);
        String location = created.getHeaders().getLocation().toString();

        var response = rest.getForEntity(location, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} returns 404 when user does not exist")
    void getUser_withNonExistentId_returns404() {
        var response = rest.getForEntity("/api/v1/users/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DELETE /api/v1/users/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/users/{id} returns 204 when user exists")
    void deleteUser_withExistingId_returns204() {
        var created = rest.postForEntity(
                "/api/v1/users", new CreateUserRequest("eve@example.com", "Eve"), String.class);
        String location = created.getHeaders().getLocation().toString();

        var response = rest.exchange(location, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} returns 404 when user does not exist")
    void deleteUser_withNonExistentId_returns404() {
        var response = rest.exchange(
                "/api/v1/users/99999", HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
