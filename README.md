# SafeHaven Service

A complete Spring Boot microservice for managing SafeHaven entities with OAuth, identity, and refresh token functionality.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 4.0.1
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA
- **Mapping**: MapStruct 1.5.5.Final
- **Validation**: Jakarta Validation
- **Utilities**: Lombok

## Project Structure

See [PACKAGE_TREE.md](./PACKAGE_TREE.md) for a complete package tree overview.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or use Docker Compose)

## Setup

1. **Database Setup**

   Update `src/main/resources/application.yml` with your PostgreSQL credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/safehaven_db
       username: postgres
       password: postgres
   ```

   Or use Docker Compose (if `compose.yaml` is configured):
   ```bash
   docker-compose up -d
   ```

2. **Build the Project**
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   ```bash
   mvn spring-boot:run
   ```

   The service will start on `http://localhost:8080`

## API Endpoints

Base path: `/api/v1/safehavens`

### Create SafeHaven
```http
POST /api/v1/safehavens
Content-Type: application/json

{
  "reference": "REF-001",
  "ownerName": "John Doe",
  "ownerEmail": "john.doe@example.com",
  "balance": 1000.00
}
```

### Get SafeHaven by ID
```http
GET /api/v1/safehavens/{id}
```

### Get SafeHaven by Reference
```http
GET /api/v1/safehavens/reference/{reference}
```

### Get All SafeHavens (with pagination)
```http
GET /api/v1/safehavens?page=0&size=20&sort=createdAt,desc
```

### Update SafeHaven
```http
PUT /api/v1/safehavens/{id}
Content-Type: application/json

{
  "ownerName": "Jane Doe",
  "ownerEmail": "jane.doe@example.com",
  "balance": 1500.00
}
```

### Suspend SafeHaven
```http
PATCH /api/v1/safehavens/{id}/suspend
```

## Business Rules

1. **Reference Uniqueness**: The `reference` field must be unique across all SafeHavens
2. **Suspended SafeHaven**: A suspended SafeHaven cannot be updated
3. **Balance Validation**: Balance must not be negative

## Error Handling

The service provides comprehensive error handling with meaningful error responses:

- **404 Not Found**: SafeHaven not found
- **409 Conflict**: Duplicate reference
- **400 Bad Request**: Validation errors or illegal operations
- **500 Internal Server Error**: Unexpected errors

Error response format:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error message",
  "path": "/api/v1/safehavens",
  "fieldErrors": [
    {
      "field": "balance",
      "message": "Balance must not be negative",
      "rejectedValue": -100
    }
  ]
}
```

## Features

- ✅ Layered architecture with clean separation of concerns
- ✅ RESTful API with proper HTTP status codes
- ✅ Request validation using Jakarta Validation
- ✅ MapStruct for entity-DTO mapping
- ✅ JPA auditing for automatic timestamp management
- ✅ Global exception handling
- ✅ Pagination support
- ✅ Comprehensive logging
- ✅ Business rule enforcement

## Testing

Run tests:
```bash
mvn test
```

## Notes

This service is part of the SafeHaven provider ecosystem, handling:
- SafeHaven identity management
- OAuth flows
- Refresh token management

All components share keys, flows, and webhook events while maintaining isolated lifecycle and secrets from other providers.
