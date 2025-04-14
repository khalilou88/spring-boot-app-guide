package com.example.api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(initializers = ApiApplicationTests.TestContainersInitializer.class)
public class ApiApplicationTests {

    @Container
    static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine").withDatabaseName("testdb").withUsername("testuser").withPassword("testpassword");

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    protected String baseUrl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Initially use 'none' to prevent Hibernate from modifying the schema
        // This will be changed to 'validate' after Flyway creates the schema
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    static class TestContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            // Nothing needed here, just using this to establish test execution order
        }
    }


    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";

        // Manually apply migrations for tests
        Flyway flyway = Flyway.configure().dataSource(postgresContainer.getJdbcUrl(), postgresContainer.getUsername(), postgresContainer.getPassword()).locations("classpath:db/migration").load();

        // Clean and migrate to ensure fresh database state for each test
//        flyway.clean();
        flyway.migrate();

        // Now that Flyway has created the schema, we can set Hibernate to validate mode
        // This will be applied to the next Hibernate session
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                (ConfigurableApplicationContext) applicationContext,
                "spring.jpa.hibernate.ddl-auto=validate"
        );
    }

    @Test
    void contextLoads() {
        // Basic test to ensure application context loads
    }
}
