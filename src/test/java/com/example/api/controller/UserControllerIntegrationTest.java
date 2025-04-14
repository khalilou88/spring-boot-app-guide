package com.example.api.controller;

import com.example.api.AbstractIntegrationTests;
import com.example.api.dto.UserCreationDTO;
import com.example.api.dto.UserDTO;
import com.example.api.dto.UserUpdateDTO;
import com.example.api.entity.User;
import com.example.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends AbstractIntegrationTests {


    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final String baseUrl = "http://localhost:" + port + "/api";


    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateNewUser() {
        // Given
        UserCreationDTO userCreationDTO = new UserCreationDTO();
        userCreationDTO.setUsername("testuser");
        userCreationDTO.setEmail("test@example.com");
        userCreationDTO.setPassword("password123");
        userCreationDTO.setFirstName("Test");
        userCreationDTO.setLastName("User");

        // When
        ResponseEntity<UserDTO> response = restTemplate.postForEntity(baseUrl + "/users", userCreationDTO, UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getFirstName()).isEqualTo("Test");
        assertThat(response.getBody().getLastName()).isEqualTo("User");
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        createTestUser("user1", "user1@example.com");
        createTestUser("user2", "user2@example.com");

        // When
        ResponseEntity<List<UserDTO>> response = restTemplate.exchange(baseUrl + "/users", HttpMethod.GET, null, new ParameterizedTypeReference<List<UserDTO>>() {
        });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    @Test
    void shouldGetUserById() {
        // Given
        User savedUser = createTestUser("testuser", "test@example.com");

        // When
        ResponseEntity<UserDTO> response = restTemplate.getForEntity(baseUrl + "/users/" + savedUser.getId(), UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void shouldUpdateUser() {
        // Given
        User savedUser = createTestUser("testuser", "test@example.com");

        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Name");

        // When
        ResponseEntity<UserDTO> response = restTemplate.exchange(baseUrl + "/users/" + savedUser.getId(), HttpMethod.PUT, new HttpEntity<>(updateDTO), UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFirstName()).isEqualTo("Updated");
        assertThat(response.getBody().getLastName()).isEqualTo("Name");
    }

    @Test
    void shouldDeleteUser() {
        // Given
        User savedUser = createTestUser("testuser", "test@example.com");

        // When
        ResponseEntity<Void> response = restTemplate.exchange(baseUrl + "/users/" + savedUser.getId(), HttpMethod.DELETE, null, Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        // When
        ResponseEntity<UserDTO> response = restTemplate.getForEntity(baseUrl + "/users/999", UserDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashedpassword");
        user.setFirstName("Test");
        user.setLastName("User");
        return userRepository.save(user);
    }
}