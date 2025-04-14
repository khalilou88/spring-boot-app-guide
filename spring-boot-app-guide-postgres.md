# Spring Boot Application Guide with PostgreSQL

This guide will walk you through creating a robust Spring Boot application using PostgreSQL in Docker for both development and testing, Flyway for database migrations, Spring Data for persistence, TestContainers for integration testing, Maven for build management, and GitHub Actions for CI/CD.

## Table of Contents
1. [Project Setup](#project-setup)
2. [PostgreSQL Configuration](#postgresql-configuration)
3. [Database Migration with Flyway](#database-migration-with-flyway)
4. [Building Data Layer with Spring Data](#building-data-layer-with-spring-data)
5. [Testing with TestContainers](#testing-with-testcontainers)
6. [Maven Configuration](#maven-configuration)
7. [CI/CD with GitHub Actions](#cicd-with-github-actions)
8. [Best Practices](#best-practices)

## Project Setup

### Step 1: Initialize a Spring Boot Project

You can use [Spring Initializer](https://start.spring.io/) to create a new project with the following dependencies:
- Spring Web
- Spring Data JPA
- Flyway Migration
- PostgreSQL Driver
- Spring Boot DevTools
- Lombok (optional but recommended)

### Step 2: Project Structure

A recommended structure for your project:

```
src
├── main
│   ├── java
│   │   └── com
│   │       └── yourcompany
│   │           └── yourapp
│   │               ├── YourAppApplication.java
│   │               ├── config
│   │               ├── controller
│   │               ├── dto
│   │               ├── entity
│   │               ├── exception
│   │               ├── repository
│   │               └── service
│   └── resources
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-test.properties
│       ├── application-prod.properties
│       └── db
│           └── migration
└── test
    └── java
        └── com
            └── yourcompany
                └── yourapp
                    ├── controller
                    ├── repository
                    └── service
```

### Step 3: Basic Configuration

In `application.properties`:

```properties
# Core settings
spring.application.name=your-app-name
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.example.api=DEBUG
logging.level.org.springframework=INFO
logging.level.org.hibernate=INFO

# Common database properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

## PostgreSQL Configuration

### Step 1: Docker Compose for Development

Create a `docker-compose.yml` file in your project root:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: app_postgres_dev
    environment:
      POSTGRES_DB: devdb
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpassword
      PGDATA: /data/postgres
    volumes:
      - postgres_data:/data/postgres
    ports:
      - "5432:5432"
    networks:
      - postgres_network
    restart: unless-stopped

networks:
  postgres_network:
    driver: bridge

volumes:
  postgres_data:
```

### Step 2: Development Environment Configuration

Create `application-dev.properties`:

```properties
# PostgreSQL for development
spring.datasource.url=jdbc:postgresql://localhost:5432/devdb
spring.datasource.username=devuser
spring.datasource.password=devpassword
spring.datasource.driver-class-name=org.postgresql.Driver

# Flyway configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Show SQL for debugging
spring.jpa.show-sql=true
```

### Step 3: Production Environment Configuration

Create `application-prod.properties`:

```properties
# PostgreSQL for production
spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Flyway configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Turn off SQL logging in production
spring.jpa.show-sql=false
```

### Step 4: Development Setup Script

Create a `dev-setup.sh` script to make it easy to start the development environment:

```bash
#!/bin/bash

# Start PostgreSQL container
echo "Starting PostgreSQL development container..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 5

# Run the Spring Boot application with dev profile
echo "Starting Spring Boot application with dev profile..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Make the script executable:
```bash
chmod +x dev-setup.sh
```

## Database Migration with Flyway

### Step 1: Configure Flyway in pom.xml

Add the dependency (should already be there if you used Spring Initializer):

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Step 2: Create Migration Scripts

Create migration scripts in `src/main/resources/db/migration` following the naming convention:
`V{version_number}__{description}.sql`

For example:

**V1__Create_Users_Table.sql:**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**V2__Create_Products_Table.sql:**
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_name ON products(name);
```

### Step 3: Flyway Best Practices

1. **Never Modify Existing Migrations**: Once committed and deployed, treat migrations as immutable.
2. **Use Descriptive Names**: Make the script names clear about what they do.
3. **Keep Migrations Small**: Create focused migrations to make rollbacks easier.
4. **Include Both Up and Down Migrations**: If your version of Flyway supports it.
5. **Test Migrations**: Verify migrations work correctly before deploying.

## Building Data Layer with Spring Data

### Step 1: Create JPA Entities

**User.java:**
```java
package com.example.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**Product.java:**
```java
package com.example.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### Step 2: Create Spring Data Repositories

**UserRepository.java:**
```java
package com.example.api.repository;

import com.example.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
```

**ProductRepository.java:**
```java
package com.example.api.repository;

import com.example.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 ORDER BY p.price ASC")
    List<Product> findInStockProductsOrderByPriceAsc();
}
```

### Step 3: Create Service Layer

**UserService.java:**
```java
package com.example.api.service;

import com.example.api.entity.User;
import com.example.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createUser(User user) {
        // Add validation logic
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        // Add validation logic
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

## Testing with TestContainers

### Step 1: Add TestContainers Dependencies to pom.xml

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
```

### Step 2: Create TestContainers Configuration

**AbstractIntegrationTest.java:**
```java
package com.example.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Step 3: Create Test Environment Configuration

**application-test.properties:**
```properties
# TestContainer will provide these dynamically
spring.datasource.driver-class-name=org.postgresql.Driver

# Flyway for test
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate

# Logging for tests
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Step 4: Create Integration Tests

**UserRepositoryIntegrationTest.java:**
```java
package com.example.api.repository;

import com.example.api.AbstractIntegrationTest;
import com.example.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByUsername() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("Test");
        user.setLastName("User");

        // When
        User savedUser = userRepository.save(user);
        User foundUser = userRepository.findByUsername("testuser").orElse(null);

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }
}
```

## Maven Configuration

### Step 1: Complete pom.xml Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.yourcompany</groupId>
    <artifactId>your-app-name</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Your App Name</name>
    <description>Your application description</description>
    
    <properties>
        <java.version>17</java.version>
        <testcontainers.version>1.19.7</testcontainers.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            
            <!-- JaCoCo for code coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    
    <profiles>
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>test</id>
            <properties>
                <spring.profiles.active>test</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>
    </profiles>
</project>
```

## CI/CD with GitHub Actions

### Step 1: Create GitHub Workflows Directory

Create a directory `.github/workflows` in your project root.

### Step 2: Create CI Workflow

**ci.yml:**
```yaml
name: Java CI with Maven

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    services:
      # We don't need to specify PostgreSQL here because TestContainers will handle it
      # This is just to show how you could add service containers if needed
      redis:
        image: redis:latest
        ports:
          - 6379:6379

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn -B package --file pom.xml
    
    - name: Run tests
      run: mvn test
    
    - name: Generate JaCoCo coverage report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: true
```

### Step 3: Create CD Workflow

**cd.yml:**
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: mvn -B package --file pom.xml -P prod
    
    - name: Run tests
      run: mvn test
    
    # Here you would add steps to deploy to your server
    # This is just a placeholder example for AWS Elastic Beanstalk
    - name: Deploy to AWS Elastic Beanstalk
      if: success()
      uses: einaregilsson/beanstalk-deploy@v21
      with:
        aws_access_key: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws_secret_key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        application_name: your-app-name
        environment_name: your-environment-name
        version_label: ${{ github.sha }}
        region: us-east-1
        deployment_package: target/your-app-name-0.0.1-SNAPSHOT.jar
```

## Best Practices

### Docker & PostgreSQL Best Practices

1. **Use Official Images**: Always use official PostgreSQL Docker images.
2. **Use Specific Version Tags**: Instead of using `latest`, use specific version tags like `postgres:15-alpine`.
3. **Persistent Volumes**: Use named volumes for data persistence across container restarts.
4. **Environment Variables**: Use environment variables for sensitive information.
5. **Network Isolation**: Use Docker networks to isolate database containers.
6. **Regular Backups**: Set up regular backups for your PostgreSQL database.
7. **Connection Pooling**: Configure appropriate connection pool settings in your application.
8. **Health Checks**: Implement health checks for your PostgreSQL containers.

### Code Organization

1. **Package by Feature**: Consider organizing code by feature rather than by technical layer for better maintainability.
2. **Use DTOs**: Transfer data between layers using Data Transfer Objects to decouple entities from the presentation layer.
3. **Repository Pattern**: Use Spring Data repositories for database access.
4. **Service Layer**: Implement business logic in service classes.
5. **Controller Layer**: Keep controllers thin, delegating business logic to services.

### Testing Strategy

1. **Test Pyramid**: Follow the test pyramid with more unit tests than integration tests.
2. **Unit Tests**: Test individual components in isolation.
3. **Integration Tests**: Use TestContainers for database integration tests.
4. **API Tests**: Test REST endpoints using MockMvc or TestRestTemplate.
5. **Code Coverage**: Aim for at least 80% test coverage.

### Security Best Practices

1. **Input Validation**: Validate all input from external sources.
2. **HTTPS**: Use HTTPS for all communications.
3. **Authentication**: Implement proper authentication (JWT, OAuth2, etc.).
4. **Authorization**: Use Spring Security for role-based access control.
5. **Sensitive Data**: Don't log sensitive data and protect it in the database.

### Database Best Practices

1. **Database Versioning**: Use Flyway to version and manage database schema changes.
2. **Connection Pooling**: Configure appropriate connection pool settings.
3. **Transactions**: Use `@Transactional` for data consistency.
4. **Query Optimization**: Write efficient queries and use indexes appropriately.
5. **Pagination**: Implement pagination for large result sets.

### CI/CD Best Practices

1. **Automated Tests**: Run tests on every build.
2. **Code Quality**: Integrate SonarQube or similar tools for code quality checks.
3. **Security Scanning**: Use tools like OWASP Dependency Check.
4. **Environment Configuration**: Use different configurations for different environments.
5. **Backup Strategy**: Implement a robust backup strategy for production data.
