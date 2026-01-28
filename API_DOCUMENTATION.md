# SafeHaven Service - API Documentation

## Overview

This document describes the Swagger/OpenAPI 3 documentation implementation for SafeHaven Service.

## Access Points

Once the application is running, access the API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

## What Was Implemented

### 1. Dependencies Added (pom.xml)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Why springdoc-openapi?**
- Modern, actively maintained (springfox is deprecated)
- Native OpenAPI 3 support
- Works seamlessly with Spring Boot 3.x/4.x
- Auto-configuration with minimal setup

### 2. OpenAPI Configuration (OpenApiConfig.java)

**Location**: `src/main/java/org/planmoni/safehavenservice/config/OpenApiConfig.java`

**Features**:
- Global API metadata (title, description, version)
- Contact information for API consumers
- Multiple server environments (local, production)
- Centralized OpenAPI bean configuration

### 3. Controller Documentation (SafeHavenController.java)

All endpoints are annotated with:

- **@Tag**: Groups endpoints in Swagger UI under "SafeHaven"
- **@Operation**: Provides summary and detailed description for each endpoint
- **@ApiResponses**: Documents all possible HTTP responses (200, 201, 400, 404, 409, 500)
- **@Parameter**: Describes path and query parameters with examples

### 4. DTO Schema Documentation

All request/response DTOs are annotated with:

- **@Schema (class-level)**: Describes the DTO's purpose
- **@Schema (field-level)**: Documents each field with:
  - Description
  - Example values
  - Data constraints (min, max, format)
  - Required/optional indicators
  - Access mode (READ_ONLY for response fields)

**Annotated DTOs**:
- `SafeHavenCreateRequest` - Create account request body
- `SafeHavenUpdateRequest` - Update account request body
- `SafeHavenResponse` - Account details response
- `ErrorResponse` - Standard error response
- `Status` - Account status enum

### 5. Application Configuration (application.yml)

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha           # Sort tags alphabetically
    operations-sorter: alpha     # Sort operations alphabetically
    display-request-duration: true
    doc-expansion: none          # Collapse all by default
  show-actuator: false           # Hide actuator endpoints
  packages-to-scan: org.planmoni.safehavenservice.controller
  paths-to-match: /api/**
```

## Best Practices Applied

### ✅ 1. Comprehensive HTTP Status Documentation

Every endpoint documents:
- **2xx**: Success responses (200 OK, 201 Created)
- **4xx**: Client errors (400 Bad Request, 404 Not Found, 409 Conflict)
- **5xx**: Server errors (500 Internal Server Error)

### ✅ 2. Validation Constraints in Documentation

Swagger UI automatically shows:
- Required fields (from `@NotNull`, `@NotBlank`)
- Length constraints (from `@Size`)
- Format validation (from `@Email`, `@DecimalMin`)
- Pattern constraints (from `@Pattern`)

### ✅ 3. Example Values

All fields include realistic example values:
```java
@Schema(
    description = "Email address of the account owner",
    example = "john.doe@example.com",
    format = "email"
)
```

### ✅ 4. Read-Only Fields

Response-only fields are marked with `accessMode = Schema.AccessMode.READ_ONLY`:
- `id`, `createdAt`, `updatedAt`
- Prevents confusion about what can be sent in requests

### ✅ 5. Logical Grouping

Endpoints are grouped under the "SafeHaven" tag for clean organization.

### ✅ 6. Pagination Documentation

The paginated endpoint includes detailed documentation on query parameters:
- `page`: Page number (0-indexed)
- `size`: Items per page
- `sort`: Sort field and direction

### ✅ 7. Error Response Consistency

All error responses use the same `ErrorResponse` schema:
```json
{
  "timestamp": "2024-01-27T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/safehaven",
  "fieldErrors": [
    {
      "field": "ownerEmail",
      "message": "Owner email must be a valid email address",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### ✅ 8. Security Considerations

- Internal implementation details are not exposed
- Sensitive fields are excluded from documentation
- Only public API endpoints are documented (`paths-to-match: /api/**`)
- Actuator endpoints are hidden (`show-actuator: false`)

## Testing the Documentation

### 1. Start the Application

```bash
mvn spring-boot:run
```

### 2. Access Swagger UI

Open in browser: http://localhost:8080/swagger-ui.html

### 3. Try It Out

Swagger UI provides an interactive interface to:
- View all endpoints
- See request/response schemas
- Execute API calls directly from the browser
- View example responses

### 4. Test Each Endpoint

Click "Try it out" on any endpoint to:
1. Fill in example values
2. Execute the request
3. See the actual response
4. View response headers and status codes

## Deployment Considerations

### Production Environment

For production deployments:

1. **Disable Swagger UI** (optional for security):
```yaml
springdoc:
  swagger-ui:
    enabled: false
```

2. **Restrict Access** using Spring Security:
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")
);
```

3. **Update Server URLs** in `OpenApiConfig.java`:
```java
new Server()
    .url("https://api.planmoni.org")
    .description("Production Server")
```

### API Versioning

Current version: `v1` (in `/api/v1/safehaven`)

When creating new versions:
- Keep existing v1 endpoints for backward compatibility
- Create new controllers for v2 under `/api/v2/`
- Document breaking changes in release notes

## Frontend Integration

### TypeScript Client Generation

Generate a TypeScript client from OpenAPI spec:

```bash
# Using openapi-generator
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o ./generated-client
```

### Direct API Consumption

Frontend applications can:
1. Fetch OpenAPI spec: `GET /v3/api-docs`
2. Generate client code automatically
3. Get type-safe API clients with IntelliSense

## Maintenance

### Adding New Endpoints

When adding new endpoints:

1. Annotate the controller method:
```java
@Operation(summary = "...", description = "...")
@ApiResponses(value = { ... })
@GetMapping("/new-endpoint")
public ResponseEntity<ResponseDTO> newEndpoint() { ... }
```

2. Annotate the DTOs:
```java
@Schema(description = "...")
public class NewDTO {
    @Schema(description = "...", example = "...")
    private String field;
}
```

3. Document all HTTP status codes
4. Provide example values
5. Test in Swagger UI

### Updating Documentation

- Update version in `OpenApiConfig.java` when releasing
- Update descriptions when API behavior changes
- Keep examples realistic and up-to-date
- Review documentation during code reviews

## Additional Resources

- [springdoc-openapi Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)
- [Jakarta Validation Annotations](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/)

## Support

For questions or issues with API documentation:
- Contact: engineering@planmoni.org
- Internal Wiki: [Link to internal documentation]
- Slack: #api-documentation
