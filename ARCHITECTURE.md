# SafeHaven Integration Service - Architecture Documentation

## Overview

This is a production-ready Spring Boot microservice for integrating with SafeHaven, bundling three related concerns:
- **safehaven-identity**: Identity mapping management
- **safehaven-oauth**: OAuth 2.0 token management
- **safehaven-refresh-token**: Refresh token lifecycle management

## Architecture Decisions

### Database Layer

#### PostgreSQL JDBC Driver
**Decision**: Use PostgreSQL JDBC driver (`org.postgresql:postgresql`)  
**Justification**:
- Official PostgreSQL driver, fully supported and actively maintained
- Complete PostgreSQL feature support (JSON types, arrays, advanced data types)
- Production-proven reliability and performance
- Full compatibility with PostgreSQL 12+ (enterprise-grade database)

#### HikariCP Connection Pool
**Decision**: Use HikariCP as the default connection pool (included with Spring Boot)  
**Justification**:
- Default Spring Boot connection pool (no additional dependencies)
- Industry-leading performance (benchmarked faster than alternatives)
- Excellent connection leak detection and monitoring
- Automatic health checks and connection validation
- Production-ready with extensive tuning options

**Configuration Highlights**:
- Pool sizing tuned for microservice workloads (5-20 connections)
- Connection health monitoring (leak detection, validation)
- Prepared statement caching for performance
- Batch statement optimization
- Connection lifetime management

#### Flyway Database Migrations
**Decision**: Use Flyway for schema versioning  
**Justification**:
- Version-controlled database schema changes
- Automatic migration on application startup
- Baseline support for existing databases
- Production-safe with rollback support
- Clear migration history and audit trail

**Migration Strategy**:
- Sequential versioning (V1, V2, V3...)
- Each migration is atomic and idempotent
- Includes indexes, constraints, and comments
- Utility functions and triggers included

### Database Schema Design

#### Provider-Scoped Isolation
All SafeHaven-specific tables are prefixed with `safehaven_` to ensure:
- Clear separation from other provider integrations
- Easy identification in database tools
- Prevents accidental cross-provider data access
- Supports multi-provider architecture

#### Key Tables

1. **safehaven_identity_mappings**
   - Maps SafeHaven user IDs to internal system user IDs
   - Enforces provider isolation
   - Supports soft delete for audit trail

2. **oauth_access_tokens**
   - Stores encrypted access tokens
   - Foreign key to identity mappings (cascade delete)
   - Expiry tracking and status management
   - Indexed for fast lookup

3. **refresh_tokens**
   - Long-lived tokens for access token refresh
   - Supports token rotation (replaced_by_token_id)
   - Expiry tracking independent of access tokens

4. **webhook_event_logs**
   - Idempotency enforcement via unique event_id
   - Full audit trail (payload, headers, signature)
   - Processing status tracking
   - Retry support via attempt_count

### Security Architecture

#### Webhook Signature Validation
**Implementation**: HMAC-SHA256 signature validation  
**Security Considerations**:
- Constant-time comparison to prevent timing attacks
- Secret stored in environment variables (never in code)
- Fail-secure: reject if signature invalid
- Comprehensive logging for security monitoring

**Signature Flow**:
1. Extract signature from `X-SafeHaven-Signature` header
2. Compute HMAC-SHA256 of payload using webhook secret
3. Constant-time comparison with received signature
4. Reject if invalid, process if valid

#### Credential Security
- All credentials via environment variables
- No hardcoded secrets
- Supports integration with secret management systems (Vault, AWS Secrets Manager)
- Production-ready for Kubernetes secrets

### Idempotency Design

#### Strategy: Database Unique Constraint
**Implementation**: Unique constraint on `event_id` in `webhook_event_logs` table

**Flow**:
1. Extract event ID from `X-SafeHaven-Event-Id` header
2. Check if event_id exists in database
3. If exists and processed successfully → return idempotent response
4. If exists but failed → allow retry (with attempt tracking)
5. If not exists → create new event log and process

**Benefits**:
- Atomic duplicate detection (database-level constraint)
- Race condition safe (database handles concurrency)
- No external dependencies (no Redis/Kafka needed)
- Full audit trail (all attempts logged)

**Idempotency Guarantees**:
- Same event_id → processed at most once successfully
- Retries supported for transient failures
- Duplicate events rejected automatically

### Async Processing Architecture

#### Design: @Async with Dedicated Thread Pool
**Implementation**: Spring `@Async` with isolated thread pools

**Benefits**:
- Non-blocking webhook responses (fast HTTP responses)
- Isolated thread pool for webhook processing (doesn't affect other async tasks)
- Graceful shutdown support (await termination)
- Configurable pool sizing per environment

**Thread Pools**:
1. **webhookProcessingExecutor**: Dedicated pool for webhook events
   - Core size: 5, Max size: 10, Queue: 100
   - Configurable via environment variables

2. **taskExecutor**: General purpose async tasks
   - Smaller pool (half of webhook pool)
   - Isolated from webhook processing

**Error Handling**:
- Failed events stored in database with error message
- Processing status tracked (PENDING, PROCESSING, SUCCESS, FAILED)
- Retry support via attempt_count
- Dead letter queue ready (can be extended)

### Event Routing Architecture

#### Pattern: Registry-Based Event Routing
**Implementation**: `WebhookEventHandlerRegistry` with handler registration

**Flow**:
1. Event received → Event type extracted from payload
2. Router looks up handler for event type
3. Handler processes event (e.g., `handleIdentityCreated`)
4. Domain events can be published (extensible)

**Extensibility**:
- New event types: Register handler in registry
- Pattern matching: Supports wildcards (e.g., `identity.*`)
- Default handler: Unknown event types handled gracefully
- Easy to extend without modifying core routing logic

**Event Types Supported**:
- `identity.*`: Identity lifecycle events
- `token.*`: Token lifecycle events
- `payment.*`: Payment events
- `account.*`: Account events

### Configuration Management

#### Environment Variable Strategy
**All configuration via environment variables** for:
- Security (no secrets in code)
- Flexibility (different values per environment)
- Container/Kubernetes readiness
- Secret management integration

**Configuration Categories**:
1. **Database**: Connection, pool settings
2. **SafeHaven OAuth**: Client credentials, endpoints
3. **Webhook**: Secret, headers, timeout
4. **Async**: Thread pool sizes, queue capacity
5. **Logging**: Log levels per component

### Package Structure

```
org.planmoni.safehavenservice/
├── config/              # Configuration classes
│   ├── AsyncConfig      # Async processing configuration
│   └── JpaAuditingConfig
├── controller/          # REST controllers
│   ├── SafeHavenController
│   └── SafeHavenWebhookController
├── entity/              # JPA entities
│   ├── identity/        # Identity mapping entities
│   ├── oauth/           # OAuth token entities
│   ├── webhook/         # Webhook event entities
│   └── SafeHaven        # Core SafeHaven entity
├── repository/          # JPA repositories
│   ├── identity/
│   ├── oauth/
│   └── webhook/
├── service/             # Business logic
│   ├── webhook/         # Webhook processing services
│   └── impl/
└── dto/                 # Data transfer objects
```

**Design Principles**:
- Package by feature (identity, oauth, webhook)
- Clear separation of concerns
- Easy to navigate and extend

### Production Readiness Features

#### Health Checks
- `/actuator/health/db` - Database connectivity
- `/actuator/health/diskspace` - Disk space monitoring
- `/api/v1/safehaven/webhooks/health` - Webhook processing health

#### Monitoring
- HikariCP metrics via Actuator
- Processing statistics (pending, success, failed counts)
- Connection pool metrics
- Custom webhook health endpoint

#### Error Handling
- Global exception handler
- Structured error responses
- Comprehensive logging
- Failed event tracking

#### Graceful Shutdown
- Connection pool graceful shutdown
- Async task completion wait
- Flyway migration rollback safe
- Database connection cleanup

### Deployment Considerations

#### Database Requirements
- PostgreSQL 12+ (tested with 12, 13, 14, 15)
- Network connectivity from application to database
- Database user with DDL permissions (for Flyway)
- SSL/TLS recommended for production

#### Environment Variables Required
- Database connection (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- SafeHaven credentials (`SAFEHAVEN_CLIENT_ID`, `SAFEHAVEN_CLIENT_SECRET`)
- Webhook secret (`SAFEHAVEN_WEBHOOK_SECRET`)

#### Recommended Production Settings
- Connection pool: `DB_POOL_MAX_SIZE=50` (scales with load)
- SSL: Enable `sslmode=require` in connection string
- Secrets: Use secret management system (Vault, AWS Secrets Manager)
- Monitoring: Enable Actuator endpoints with authentication
- Logging: Structured logging (JSON format recommended)

### Extension Points

#### Adding New Event Types
1. Register handler in `WebhookEventHandlerRegistry.init()`
2. Implement handler method (e.g., `handleNewEventType`)
3. No changes needed to router or controller

#### Token Encryption (Not Implemented)
- Current implementation stores tokens in plain text (for development)
- Production should encrypt tokens at rest
- Recommended: AES-256 encryption
- Keys stored in secret management system

#### Dead Letter Queue (Not Implemented)
- Failed events currently stored in database
- Can be extended to publish to message queue (Kafka, RabbitMQ)
- Enables manual review and replay

#### OAuth Token Refresh Job
- Can add scheduled job to refresh expired tokens
- Use `@Scheduled` annotation
- Token expiry tracked in database

### Testing Recommendations

1. **Unit Tests**: Service layer with mocked repositories
2. **Integration Tests**: Repository layer with test database
3. **Webhook Tests**: Mock HTTP requests with signature validation
4. **Idempotency Tests**: Duplicate event handling
5. **Database Tests**: Flyway migration testing

### Future Enhancements

1. **Token Encryption**: AES-256 encryption for tokens at rest
2. **Dead Letter Queue**: Failed event publishing to message queue
3. **Metrics**: Custom metrics for webhook processing (Prometheus)
4. **Retry Mechanism**: Automatic retry for failed events
5. **Rate Limiting**: Protect webhook endpoint from abuse
6. **Event Replay**: Manual replay of processed events
7. **Multi-tenant Support**: Support for multiple SafeHaven accounts
