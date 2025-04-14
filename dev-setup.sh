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