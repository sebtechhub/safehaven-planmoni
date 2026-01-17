# Docker Compose Integration Fix Guide

## Root Cause Analysis

### The Problem

Spring Boot 3.x includes **automatic Docker Compose integration** that:

1. **Auto-detects** `compose.yaml` or `docker-compose.yml` files in your project root
2. **Auto-starts** Docker Compose services on application startup
3. **Auto-stops** services on application shutdown (if configured)

### Why It's Failing

**On Windows**, Spring Boot tries to connect to Docker Desktop via a named pipe:
```
//./pipe/dockerDesktopLinuxEngine
```

**Failure scenarios:**
- Docker Desktop is **not running** → Named pipe doesn't exist → Connection error
- Docker Desktop is starting up → Named pipe not ready → Connection timeout
- Docker context switched → Wrong Docker context → Connection error
- WSL2 integration issues → Named pipe path incorrect → Connection error

### Error Stack Trace Analysis

```
org.springframework.boot.docker.compose.*
  └── DockerComposeLifecycleManager
      └── DockerCli
          └── Attempts to connect to Docker Engine
              └── Named pipe: //./pipe/dockerDesktopLinuxEngine
                  └── ERROR: The system cannot find the file specified
```

## Solution Options

### ✅ **Option 1: Disable via Configuration (Recommended for Production)**

**Best for:** Production deployments where Docker Compose should never run

**Implementation:**

```yaml
# application.yml (already applied)
spring:
  docker:
    compose:
      enabled: false  # Disables auto-start
```

**Or via environment variable:**
```bash
SPRING_DOCKER_COMPOSE_ENABLED=false
```

**Pros:**
- ✅ Production-safe (defaults to disabled)
- ✅ No code changes needed
- ✅ Environment variable override support
- ✅ Can be re-enabled per environment

**Cons:**
- ⚠️ Still includes dependency (but harmless when disabled)

---

### ✅ **Option 2: Profile-Based Configuration (Recommended for Development)**

**Best for:** Development teams who want Docker Compose in local dev but not in other environments

**Implementation:**

**application.yml (base - Docker Compose disabled):**
```yaml
spring:
  docker:
    compose:
      enabled: false  # Disabled by default
```

**application-local.yml (local profile - Docker Compose enabled):**
```yaml
spring:
  docker:
    compose:
      enabled: true  # Enabled only for local profile
      lifecycle-management: start_and_stop
```

**Activate local profile:**
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run

# Or in IDE run configuration
-Dspring.profiles.active=local
```

**Pros:**
- ✅ Clear separation of concerns
- ✅ Production-safe (disabled by default)
- ✅ Easy local development workflow
- ✅ No environment variable needed in production

**Cons:**
- ⚠️ Requires profile activation for local development

---

### ✅ **Option 3: Remove Dependency (Most Production-Safe)**

**Best for:** Production-only deployments where Docker Compose is never needed

**Implementation:**

**Remove from pom.xml:**
```xml
<!-- REMOVE THIS DEPENDENCY -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Pros:**
- ✅ No Docker Compose integration at all
- ✅ Smaller JAR size
- ✅ No possibility of accidental activation

**Cons:**
- ⚠️ If you need it later, must add back
- ⚠️ Requires Maven rebuild

---

### ✅ **Option 4: Conditional on Docker Availability**

**Best for:** Applications that should auto-start Docker Compose only when Docker is available

**Implementation - Create a custom configuration class:**

```java
@Configuration
@ConditionalOnProperty(
    name = "spring.docker.compose.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConditionalOnClass(DockerComposeLifecycleManager.class)
public class DockerComposeConditionalConfig {
    
    @Bean
    @ConditionalOnProperty(
        name = "spring.docker.compose.auto-detect",
        havingValue = "true",
        matchIfMissing = false
    )
    public DockerComposeLifecycleManager dockerComposeLifecycleManager(
            DockerComposeProperties properties) {
        // Only created if Docker is actually available
        return new DockerComposeLifecycleManager(properties);
    }
}
```

**Pros:**
- ✅ Graceful degradation
- ✅ No errors if Docker unavailable

**Cons:**
- ⚠️ More complex
- ⚠️ Usually unnecessary (Option 1 is simpler)

---

## Production Configuration (Recommended)

### For Fintech Microservice Deployment

**Recommended approach: Multi-layered configuration**

1. **Base configuration (application.yml)** - Docker Compose disabled:
```yaml
spring:
  docker:
    compose:
      enabled: false  # Never auto-start in production
```

2. **Environment variable override** - Production deployment:
```bash
# Kubernetes Deployment / Docker Compose / Systemd
SPRING_DOCKER_COMPOSE_ENABLED=false
```

3. **Local development profile** - Only when needed:
```bash
# Activate local profile for Docker Compose
SPRING_PROFILES_ACTIVE=local
```

### Environment-Specific Examples

**Local Development (with Docker Desktop running):**
```bash
# Option A: Enable via profile
SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# Option B: Enable via environment variable
SPRING_DOCKER_COMPOSE_ENABLED=true
mvn spring-boot:run
```

**Local Development (without Docker Desktop):**
```bash
# Docker Compose disabled (default)
mvn spring-boot:run
# Application connects to existing PostgreSQL at DB_URL
```

**CI/CD Pipeline:**
```yaml
# Jenkins/GitHub Actions
env:
  SPRING_DOCKER_COMPOSE_ENABLED: false
  DB_URL: jdbc:postgresql://test-db:5432/testdb
```

**Staging/Production (Kubernetes):**
```yaml
# Kubernetes Deployment
env:
  - name: SPRING_DOCKER_COMPOSE_ENABLED
    value: "false"
  - name: DB_URL
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: url
```

---

## Windows-Specific Fixes

### Issue: Docker Desktop Engine Not Available

**Symptoms:**
- Error: `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`
- Spring Boot startup fails before application context loads

**Solutions:**

#### 1. Ensure Docker Desktop is Running
```powershell
# Check if Docker Desktop is running
docker version

# Start Docker Desktop if not running
# (Launch Docker Desktop application manually)
```

#### 2. Verify Docker Context
```powershell
# List Docker contexts
docker context ls

# Use default context
docker context use default

# Verify Docker is accessible
docker ps
```

#### 3. Check WSL2 Integration (if using WSL2 backend)
```powershell
# Verify WSL2 integration is enabled in Docker Desktop
# Settings → Resources → WSL Integration
# Enable integration for your WSL2 distribution

# Restart Docker Desktop after enabling
```

#### 4. Use Docker Desktop's TCP Socket (Alternative)
If named pipe continues to fail, configure Spring Boot to use TCP:

**application-local.yml:**
```yaml
spring:
  docker:
    compose:
      enabled: true
      host: tcp://localhost:2375  # Docker Desktop TCP port (if enabled)
      # Note: This requires enabling TCP port in Docker Desktop settings
      # Settings → General → Expose daemon on tcp://localhost:2375 without TLS
```

**⚠️ Security Warning:** TCP without TLS is insecure. Only use for local development.

#### 5. Disable Docker Compose (Recommended)
If Docker Desktop is unreliable or not needed:
```yaml
spring:
  docker:
    compose:
      enabled: false  # Prevents all Docker Compose integration
```

---

## Troubleshooting Steps

### Step 1: Verify Current Configuration
```bash
# Check if Docker Compose is enabled
# Look for spring.docker.compose.enabled in application.yml

# Check environment variables
echo $SPRING_DOCKER_COMPOSE_ENABLED  # Linux/Mac
$env:SPRING_DOCKER_COMPOSE_ENABLED   # Windows PowerShell
```

### Step 2: Test Docker Desktop Connection
```powershell
# Windows PowerShell
docker version
docker ps

# If these fail, Docker Desktop is not accessible
```

### Step 3: Verify Compose File Exists
```bash
# Check if compose.yaml exists (triggers auto-detection)
ls compose.yaml
# or
ls docker-compose.yml
```

### Step 4: Check Spring Boot Logs
Look for these log messages:
```
# When Docker Compose is enabled but Docker unavailable:
ERROR o.s.b.d.c.DockerComposeLifecycleManager - Failed to start Docker Compose

# When Docker Compose is disabled:
INFO o.s.b.d.c.DockerComposeLifecycleManager - Docker Compose support is disabled
```

---

## Configuration Reference

### Complete Docker Compose Configuration Options

```yaml
spring:
  docker:
    compose:
      enabled: true                    # Enable/disable Docker Compose
      file: compose.yaml               # Compose file path (auto-detected if not specified)
      lifecycle-management: start_and_stop  # none | start_only | start_and_stop
      host: unix:///var/run/docker.sock    # Docker host (auto-detected on Windows/Mac/Linux)
      
      # Service health checks
      wait:
        enabled: true                  # Wait for services to be healthy
        timeout: 60s                   # Maximum wait time
        
        # Wait for specific service
        services:
          postgres:
            enabled: true
            tcp:
              ports:
                - 5432               # Wait for port 5432 to be available
            healthcheck:              # Wait for health check to pass
              enabled: true
```

### Environment Variable Overrides

```bash
# Disable Docker Compose
SPRING_DOCKER_COMPOSE_ENABLED=false

# Enable Docker Compose
SPRING_DOCKER_COMPOSE_ENABLED=true

# Specify compose file
SPRING_DOCKER_COMPOSE_FILE=compose.yaml

# Lifecycle management
SPRING_DOCKER_COMPOSE_LIFECYCLE_MANAGEMENT=start_and_stop

# Docker host (rarely needed)
SPRING_DOCKER_COMPOSE_HOST=unix:///var/run/docker.sock
```

---

## Best Practice Recommendations

### For Production Fintech Microservice

1. **Default to Disabled**
   ```yaml
   spring:
     docker:
       compose:
         enabled: false  # Never auto-start in production
   ```

2. **Use Profile for Local Development**
   - Create `application-local.yml` with Docker Compose enabled
   - Activate only when needed: `SPRING_PROFILES_ACTIVE=local`

3. **Environment Variable Override**
   - Production deployments: `SPRING_DOCKER_COMPOSE_ENABLED=false`
   - CI/CD: Explicitly set to false

4. **Database Configuration**
   - Production: Use managed database (AWS RDS, Azure Database, etc.)
   - Local: Either Docker Compose OR external PostgreSQL
   - Never rely on Docker Compose for production database

5. **Monitoring**
   - Check application logs for Docker Compose initialization
   - Alert if Docker Compose is enabled in production (should never be)

---

## Verification

After applying fixes, verify:

1. **Application starts without Docker Desktop:**
   ```bash
   # Stop Docker Desktop, then:
   mvn spring-boot:run
   # Should start successfully
   ```

2. **Docker Compose is disabled:**
   ```bash
   # Check logs for:
   # "Docker Compose support is disabled" or no Docker Compose logs
   ```

3. **Database connection works:**
   ```bash
   # Application should connect to database specified in DB_URL
   # Not attempt to start Docker Compose PostgreSQL
   ```

---

## Quick Reference

| Scenario | Configuration | Command |
|----------|--------------|---------|
| Production | `enabled: false` (default) | None needed |
| Local Dev (Docker) | `enabled: true` + local profile | `SPRING_PROFILES_ACTIVE=local` |
| Local Dev (No Docker) | `enabled: false` (default) | None needed |
| CI/CD | `enabled: false` via env var | `SPRING_DOCKER_COMPOSE_ENABLED=false` |

---

## Summary

**Root Cause:** Spring Boot 3.x auto-detects `compose.yaml` and tries to connect to Docker Desktop via named pipe on Windows.

**Fix Applied:** Docker Compose disabled by default in `application.yml` with environment variable override support.

**Production Impact:** ✅ Zero - Docker Compose will not auto-start in production.

**Local Development:** Optionally enable via `application-local.yml` profile when Docker Desktop is running.
