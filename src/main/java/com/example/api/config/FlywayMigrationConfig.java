package com.example.api.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class FlywayMigrationConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /**
     * Manual migration utility that can be used in production
     * This bean is excluded from normal application startup
     */
    @Bean
    @Profile("migration")
    public CommandLineRunner flywayMigrate() {
        return args -> {
            log.info("Running manual Flyway migration...");
            Flyway flyway = Flyway.configure()
                    .dataSource(datasourceUrl, datasourceUsername, datasourcePassword)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();

            MigrateResult migrations = flyway.migrate();
            log.info("Applied {} migrations", migrations);

            // Exit after migration is complete
            System.exit(0);
        };
    }
}