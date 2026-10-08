# Smart Hospital Appointment and Priority Queue Management System

## Backend

Spring Boot REST API backend for the Smart Hospital Appointment and Priority Queue Management System.

## Features

- JWT-based authentication
- Role-based authorization
- Patient management
- Doctor management
- Department management
- Doctor schedule management
- Appointment booking and cancellation
- Priority-based patient queue
- Emergency, Urgent and Normal priority handling
- Queue position tracking
- Doctor consultation workflow
- Patient dashboard
- Doctor dashboard
- Admin dashboard

## Technology Stack

- Java 21
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- MySQL
- Maven

## Database

The `database/` directory contains the SQL database schema used by the project.

## Configuration

The application supports environment variables for deployment configuration.

Important variables include:

- `SERVER_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION`

For local development, the default configuration in `application.properties` can be used.

## Run Locally

### Windows

```powershell
.\mvnw.cmd spring-boot:run