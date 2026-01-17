# SafeHaven Service - Package Tree Overview

```
com.safehaven.safehavenservice
├── SafehavenServiceApplication.java          # Main Spring Boot application class
│
├── controller/
│   └── SafeHavenController.java              # REST controller with all endpoints
│
├── service/
│   ├── SafeHavenService.java                 # Service interface
│   └── impl/
│       └── SafeHavenServiceImpl.java          # Service implementation with business logic
│
├── repository/
│   └── SafeHavenRepository.java              # JPA repository interface
│
├── dto/
│   ├── request/
│   │   ├── SafeHavenCreateRequest.java       # DTO for creating SafeHaven
│   │   └── SafeHavenUpdateRequest.java       # DTO for updating SafeHaven
│   └── response/
│       ├── SafeHavenResponse.java             # DTO for SafeHaven response
│       └── ErrorResponse.java                # DTO for error responses
│
├── mapper/
│   └── SafeHavenMapper.java                  # MapStruct mapper interface
│
├── entity/
│   ├── SafeHaven.java                        # JPA entity
│   └── Status.java                           # Enum for SafeHaven status
│
├── exception/
│   ├── SafeHavenNotFoundException.java       # Custom exception for not found
│   ├── DuplicateReferenceException.java      # Custom exception for duplicate reference
│   ├── IllegalOperationException.java        # Custom exception for illegal operations
│   └── GlobalExceptionHandler.java           # Global exception handler with @RestControllerAdvice
│
└── config/
    └── JpaAuditingConfig.java                # JPA auditing configuration
```

## API Endpoints

Base path: `/api/v1/safehavens`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/safehavens` | Create a new SafeHaven |
| GET | `/api/v1/safehavens/{id}` | Get SafeHaven by ID |
| GET | `/api/v1/safehavens/reference/{reference}` | Get SafeHaven by reference |
| GET | `/api/v1/safehavens` | Get all SafeHavens (with pagination) |
| PUT | `/api/v1/safehavens/{id}` | Update SafeHaven |
| PATCH | `/api/v1/safehavens/{id}/suspend` | Suspend SafeHaven |

## Business Rules

1. **Reference Uniqueness**: The `reference` field must be unique across all SafeHavens
2. **Suspended SafeHaven**: A suspended SafeHaven cannot be updated
3. **Balance Validation**: Balance must not be negative

## Technology Stack

- **Java**: 17
- **Spring Boot**: 4.0.1 (latest stable)
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA
- **Mapping**: MapStruct
- **Validation**: Jakarta Validation
- **Utilities**: Lombok
