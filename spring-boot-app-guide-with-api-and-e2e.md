# Spring Boot Application Guide with API Endpoints and E2E Testing

This guide expands on the previous sections with complete API endpoints implementation and comprehensive end-to-end testing.

## Table of Contents

1. [API Endpoints](#api-endpoints)
   - [Controller Implementation](#controller-implementation)
   - [DTO Layer](#dto-layer)
   - [Exception Handling](#exception-handling)
   - [API Documentation](#api-documentation)
2. [End-to-End Testing](#end-to-end-testing)
   - [Test Containers for E2E Tests](#test-containers-for-e2e-tests)
   - [REST API Testing](#rest-api-testing)
   - [Data Validation Tests](#data-validation-tests)
   - [Security Tests](#security-tests)
3. [GitHub Actions Workflow](#github-actions-workflow)

## API Endpoints

### Controller Implementation

First, let's create RESTful controllers for our User and Product entities:

**UserController.java:**
```java
package com.example.api.controller;

import com.example.api.dto.UserCreationDTO;
import com.example.api.dto.UserDTO;
import com.example.api.dto.UserUpdateDTO;
import com.example.api.entity.User;
import com.example.api.exception.ResourceNotFoundException;
import com.example.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.findAllUsers().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreationDTO userCreationDTO) {
        User user = convertToEntity(userCreationDTO);
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(convertToDTO(createdUser), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        return userService.findUserById(id)
                .map(existingUser -> {
                    updateUserFromDTO(existingUser, userUpdateDTO);
                    User updatedUser = userService.updateUser(existingUser);
                    return new ResponseEntity<>(convertToDTO(updatedUser), HttpStatus.OK);
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.findUserById(id).isPresent()) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User convertToEntity(UserCreationDTO userCreationDTO) {
        User user = new User();
        user.setUsername(userCreationDTO.getUsername());
        user.setEmail(userCreationDTO.getEmail());
        user.setPasswordHash(userCreationDTO.getPassword()); // In a real app, hash the password!
        user.setFirstName(userCreationDTO.getFirstName());
        user.setLastName(userCreationDTO.getLastName());
        return user;
    }

    private void updateUserFromDTO(User user, UserUpdateDTO userUpdateDTO) {
        if (userUpdateDTO.getFirstName() != null) {
            user.setFirstName(userUpdateDTO.getFirstName());
        }
        if (userUpdateDTO.getLastName() != null) {
            user.setLastName(userUpdateDTO.getLastName());
        }
        if (userUpdateDTO.getEmail() != null) {
            user.setEmail(userUpdateDTO.getEmail());
        }
        // Don't update username as it's usually not changeable
        // If password is provided, it should be hashed before saving
        if (userUpdateDTO.getPassword() != null) {
            user.setPasswordHash(userUpdateDTO.getPassword()); // In a real app, hash the password!
        }
    }
}
```

**ProductController.java:**
```java
package com.example.api.controller;

import com.example.api.dto.ProductDTO;
import com.example.api.entity.Product;
import com.example.api.exception.ResourceNotFoundException;
import com.example.api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.findAllProducts().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return productService.findProductById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam(required = false) String name) {
        List<ProductDTO> products = productService.searchProductsByName(name).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/in-stock")
    public ResponseEntity<List<ProductDTO>> getInStockProducts() {
        List<ProductDTO> products = productService.findInStockProducts().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(convertToDTO(createdProduct), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        return productService.findProductById(id)
                .map(existingProduct -> {
                    updateProductFromDTO(existingProduct, productDTO);
                    Product updatedProduct = productService.updateProduct(existingProduct);
                    return new ResponseEntity<>(convertToDTO(updatedProduct), HttpStatus.OK);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productService.findProductById(id).isPresent()) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Product convertToEntity(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        return product;
    }

    private void updateProductFromDTO(Product product, ProductDTO productDTO) {
        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getStockQuantity() != null) {
            product.setStockQuantity(productDTO.getStockQuantity());
        }
    }
}
```

### DTO Layer

Let's create the DTOs (Data Transfer Objects) that we referenced in our controllers:

**UserDTO.java:**
```java
package com.example.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**UserCreationDTO.java:**
```java
package com.example.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreationDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    private String firstName;
    private String lastName;
}
```

**UserUpdateDTO.java:**
```java
package com.example.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Email(message = "Email should be valid")
    private String email;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    private String firstName;
    private String lastName;
}
```

**ProductDTO.java:**
```java
package com.example.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;
    
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Exception Handling

To properly handle exceptions across the API:

**ResourceNotFoundException.java:**
```java
package com.example.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**GlobalExceptionHandler.java:**
```java
package com.example.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(
            ResourceNotFoundException exception, WebRequest request) {
        
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                exception.getMessage(),
                request.getDescription(false));
        
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorDetails> handleValidationExceptions(
            MethodArgumentNotValidException exception) {
        
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorDetails errorDetails = new ValidationErrorDetails(
                LocalDateTime.now(),
                "Validation failed",
                errors);
        
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(
            Exception exception, WebRequest request) {
        
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                exception.getMessage(),
                request.getDescription(false));
        
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

**ErrorDetails.java:**
```java
package com.example.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
}
```

**ValidationErrorDetails.java:**
```java
package com.example.api.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class ValidationErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private Map<String, String> errors;

    public ValidationErrorDetails(LocalDateTime timestamp, String message, Map<String, String> errors) {
        this.timestamp = timestamp;
        this.message = message;
        this.errors = errors;
    }
}
```

### API Documentation

To document your API, add SpringDoc OpenAPI (formerly Swagger):

First, add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Create an OpenAPI configuration:

**OpenApiConfig.java:**
```java
package com.example.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Your App API")
                        .version("1.0.0")
                        .description("API documentation for your Spring Boot application")
                        .termsOfService("http://your-domain.com/terms")
                        .license(new License().name("Your License").url("http://your-domain.com/license")));
    }
}
```

With this setup, your API documentation will be available at:
- http://localhost:8080/swagger-ui.html
- http://localhost:8080/v3/api-docs

## End-to-End Testing

End-to-end (E2E) testing validates the entire application flow, including API endpoints, data persistence, and business logic. Let's expand our testing approach:

### Test Containers for E2E Tests

We'll continue using TestContainers for end-to-end tests but enhance our base class:

**AbstractIntegrationTest.java** (expanded version):
```java
package com.example.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### REST API Testing

Let's create comprehensive tests for our REST API:

**UserControllerIntegrationTest.java:**
```java
package com.example.api.controller;

import com.example.api.AbstractIntegrationTest;
import com.example.api.dto.UserCreationDTO;
import com.example.api.dto.UserDTO;
import com.example.api.dto.UserUpdateDTO;
import com.example.api.entity.User;
import com.example.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

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
        ResponseEntity<UserDTO> response = restTemplate.postForEntity(
                baseUrl + "/users", userCreationDTO, UserDTO.class);

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
        ResponseEntity<List<UserDTO>> response = restTemplate.exchange(
                baseUrl + "/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<UserDTO>>() {});

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
        ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                baseUrl + "/users/" + savedUser.getId(), UserDTO.class);

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
        ResponseEntity<UserDTO> response = restTemplate.exchange(
                baseUrl + "/users/" + savedUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateDTO),
                UserDTO.class);

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
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/users/" + savedUser.getId(),
                HttpMethod.DELETE,
                null,
                Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() {
        // When
        ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                baseUrl + "/users/999", UserDTO.class);

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
```

**ProductControllerIntegrationTest.java:**
```java
package com.example.api.controller;

import com.example.api.AbstractIntegrationTest;
import com.example.api.dto.ProductDTO;
import com.example.api.entity.Product;
import com.example.api.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateNewProduct() {
        // Given
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Test Product");
        productDTO.setDescription("This is a test product");
        productDTO.setPrice(new BigDecimal("19.99"));
        productDTO.setStockQuantity(100);

        // When
        ResponseEntity<ProductDTO> response = restTemplate.postForEntity(
                baseUrl + "/products", productDTO, ProductDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Product");
        assertThat(response.getBody().getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        createTestProduct("Product 1", new BigDecimal("10.00"), 10);
        createTestProduct("Product 2", new BigDecimal("20.00"), 20);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                baseUrl + "/products",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDTO>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    @Test
    void shouldSearchProductsByName() {
        // Given
        createTestProduct("Special Product", new BigDecimal("15.00"), 5);
        createTestProduct("Regular Product", new BigDecimal("10.00"), 10);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                baseUrl + "/products/search?name=Special",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDTO>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Special Product");
    }

    @Test
    void shouldGetInStockProducts() {
        // Given
        createTestProduct("In Stock", new BigDecimal("15.00"), 5);
        createTestProduct("Out of Stock", new BigDecimal("10.00"), 0);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                baseUrl + "/products/in-stock",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDTO>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("In Stock");
    }

    private Product createTestProduct(String name, BigDecimal price, int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        return productRepository.save(product);
    }
}
```

### Data Validation Tests

Let's also test data validation rules:

**UserValidationIntegrationTest.java:**
```java
package com.example.api.controller;

import com.example.api.AbstractIntegrationTest;
import com.example.api.dto.UserCreationDTO;
import com.example.api.exception.ValidationErrorDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class UserValidationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldRejectInvalidEmailFormat() {
        // Given
        UserCreation