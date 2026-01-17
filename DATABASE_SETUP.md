# Database Setup Guide

## PostgreSQL Configuration

This microservice uses **PostgreSQL** as its primary database with the following production-grade setup:

### Database Technology Stack

1. **PostgreSQL JDBC Driver** (`org.postgresql:postgresql`)
   - Official PostgreSQL JDBC driver
   - Fully compatible with PostgreSQL 12+
   - Supports all PostgreSQL features and data types

2. **HikariCP Connection Pool** (included with Spring Boot)
   - Default and recommended connection pool for Spring Boot
   - Production-grade performance and reliability
   - Automatic connection management and health checks

3. **Flyway Database Migrations**
   - Version-controlled schema migrations
   - Automatic migration on application startup
   - Baseline support for existing databases

### Configuration via Environment Variables

All database configuration is done via environment variables for security and flexibility across environments.

#### Required Environment Variables

```bash
# Database Connection
DB_URL=jdbc:postgresql://localhost:5432/safehaven_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# HikariCP Connection Pool (Optional - defaults provided)
DB_POOL_MIN_IDLE=5
DB_POOL_MAX_SIZE=20
DB_POOL_CONNECTION_TIMEOUT=30000
DB_POOL_IDLE_TIMEOUT=600000
DB_POOL_MAX_LIFETIME=1800000
DB_POOL_LEAK_DETECTION=60000
DB_POOL_VALIDATION_TIMEOUT=5000
DB_SOCKET_TIMEOUT=30

# Flyway (Optional - defaults provided)
FLYWAY_ENABLED=true
```

#### Environment-Specific Examples

**Local Development:**
```bash
DB_URL=jdbc:postgresql://localhost:5432/safehaven_db_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password
JPA_SHOW_SQL=true
HIBERNATE_SQL_LOG=true
```

**Staging:**
```bash
DB_URL=jdbc:postgresql://staging-db.example.com:5432/safehaven_db_staging
DB_USERNAME=safehaven_staging
DB_PASSWORD=${VAULT_STAGING_DB_PASSWORD}  # From secret management
DB_POOL_MAX_SIZE=30
DB_SOCKET_TIMEOUT=60
```

**Production:**
```bash
DB_URL=jdbc:postgresql://prod-db-cluster.example.com:5432/safehaven_db_prod
DB_USERNAME=safehaven_prod
DB_PASSWORD=${VAULT_PROD_DB_PASSWORD}  # From secret management
DB_POOL_MIN_IDLE=10
DB_POOL_MAX_SIZE=50
DB_POOL_CONNECTION_TIMEOUT=30000
DB_POOL_LEAK_DETECTION=60000
FLYWAY_ENABLED=true
```

### HikariCP Connection Pool Tuning

The connection pool is configured for production workloads with the following settings:

#### Pool Sizing
- **minimum-idle**: 5 (minimum connections to maintain)
- **maximum-pool-size**: 20 (maximum concurrent connections)
- **queue-capacity**: Unbounded (HikariCP uses internal queue)

#### Connection Health
- **connection-timeout**: 30 seconds (timeout for acquiring connection)
- **idle-timeout**: 10 minutes (idle connection cleanup)
- **max-lifetime**: 30 minutes (maximum connection lifetime)
- **leak-detection-threshold**: 60 seconds (detect connection leaks)
- **validation-timeout**: 5 seconds (timeout for connection validation)

#### Performance Optimizations
- **cachePrepStmts**: true (prepared statement caching)
- **prepStmtCacheSize**: 250 (prepared statement cache size)
- **prepStmtCacheSqlLimit**: 2048 (max SQL length for caching)
- **rewriteBatchedStatements**: true (optimize batch inserts)

### Database Schema

The service manages the following tables:

1. **safe_havens** - Core SafeHaven entities
2. **safehaven_identity_mappings** - Identity mappings (SafeHaven user ID → Internal user ID)
3. **oauth_access_tokens** - OAuth 2.0 access tokens
4. **refresh_tokens** - OAuth 2.0 refresh tokens
5. **webhook_event_logs** - Webhook event processing logs (idempotency)

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/`:

- `V1__Initial_schema.sql` - Initial schema (safe_havens table)
- `V2__Identity_mappings.sql` - Identity mappings table
- `V3__OAuth_tables.sql` - OAuth token tables
- `V4__Webhook_events.sql` - Webhook event logs table
- `V5__Functions_and_triggers.sql` - Database functions and triggers

Migrations run automatically on application startup. To disable:
```bash
FLYWAY_ENABLED=false
```

### Security Considerations

1. **Credentials**: Never commit database credentials to version control
2. **Encryption**: Use encrypted connections in production (`sslmode=require`)
3. **Secrets Management**: Use secret management systems (HashiCorp Vault, AWS Secrets Manager, etc.)
4. **Token Encryption**: OAuth tokens should be encrypted at rest (not implemented in this base version)
5. **Connection Security**: Use network-level security (VPC, firewall rules)

### Database Connection String Format

```
jdbc:postgresql://[host]:[port]/[database]?[parameters]
```

Example with SSL:
```
jdbc:postgresql://prod-db.example.com:5432/safehaven_db?sslmode=require&sslrootcert=/path/to/ca.crt
```

### Monitoring

HikariCP exposes metrics via Spring Boot Actuator:
- `/actuator/metrics/hikari.connections.active`
- `/actuator/metrics/hikari.connections.idle`
- `/actuator/metrics/hikari.connections.pending`

Database health check:
- `/actuator/health/db` - Database connectivity health

### Production Deployment Checklist

- [ ] Database credentials configured via environment variables
- [ ] Connection pool size tuned for expected load
- [ ] SSL/TLS enabled for database connections
- [ ] Flyway migrations tested in staging
- [ ] Database backups configured
- [ ] Connection monitoring enabled
- [ ] Connection leak detection enabled
- [ ] Connection timeout values appropriate for network latency

### Troubleshooting

**Connection Pool Exhaustion:**
- Check `DB_POOL_MAX_SIZE` value
- Monitor `hikari.connections.pending` metric
- Review application logs for connection leaks

**Slow Queries:**
- Enable SQL logging: `JPA_SHOW_SQL=true`
- Review Hibernate statistics
- Check database indexes

**Migration Failures:**
- Check Flyway logs
- Verify database user has DDL permissions
- Ensure baseline version is correct for existing databases
