package com.example.api.controller;

import com.example.api.AbstractIntegrationTests;
import com.example.api.dto.UserCreationDTO;
import com.example.api.exception.ValidationErrorDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserValidationIntegrationTest extends AbstractIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final String baseUrl = "http://localhost:" + port + "/api";


    private final String USER_API_PATH = baseUrl + "/users";

    @Test
    void shouldRejectInvalidEmailFormat() {
        // Given
        UserCreationDTO userCreationDTO = UserCreationDTO.builder().email("invalid-email-format").username("testuser").password("Password123!").firstName("John").lastName("Doe").build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, userCreationDTO, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey("email");
        assertThat(response.getBody().getErrors().get("email")).contains("valid email");
    }

    @Test
    void shouldRejectEmptyUsername() {
        // Given
        UserCreationDTO userCreationDTO = UserCreationDTO.builder().email("valid@example.com").username("").password("Password123!").firstName("John").lastName("Doe").build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, userCreationDTO, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey("username");
    }

    @Test
    void shouldRejectWeakPassword() {
        // Given
        UserCreationDTO userCreationDTO = UserCreationDTO.builder().email("valid@example.com").username("testuser").password("weak").firstName("John").lastName("Doe").build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, userCreationDTO, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey("password");
//        assertThat(response.getBody().getErrors().get("password"))
//                .anyMatch(error -> error.contains("strength") || error.contains("requirements"));
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        // Given
        UserCreationDTO userCreationDTO = UserCreationDTO.builder().email(null).username(null).password(null).firstName(null).lastName(null).build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, userCreationDTO, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKeys("email", "username", "password");
    }

    @Test
    void shouldAcceptValidUserData() {
        // Given
        UserCreationDTO userCreationDTO = UserCreationDTO.builder().email("valid@example.com").username("validuser").password("ValidPassword123!").firstName("John").lastName("Doe").build();

        // When
        ResponseEntity<Object> response = restTemplate.postForEntity(USER_API_PATH, userCreationDTO, Object.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // First create a user
        UserCreationDTO firstUser = UserCreationDTO.builder().email("duplicate@example.com").username("firstuser").password("ValidPassword123!").firstName("First").lastName("User").build();

        restTemplate.postForEntity(USER_API_PATH, firstUser, Object.class);

        // Then try to create a second user with the same email
        UserCreationDTO secondUser = UserCreationDTO.builder().email("duplicate@example.com").username("seconduser").password("ValidPassword123!").firstName("Second").lastName("User").build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, secondUser, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey("email");
        assertThat(response.getBody().getErrors().get("email")).contains("already in use");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        // First create a user
        UserCreationDTO firstUser = UserCreationDTO.builder().email("user1@example.com").username("duplicateuser").password("ValidPassword123!").firstName("First").lastName("User").build();

        restTemplate.postForEntity(USER_API_PATH, firstUser, Object.class);

        // Then try to create a second user with the same username
        UserCreationDTO secondUser = UserCreationDTO.builder().email("user2@example.com").username("duplicateuser").password("ValidPassword123!").firstName("Second").lastName("User").build();

        // When
        ResponseEntity<ValidationErrorDetails> response = restTemplate.postForEntity(USER_API_PATH, secondUser, ValidationErrorDetails.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey("username");
        assertThat(response.getBody().getErrors().get("username")).contains("already in use");
    }
}