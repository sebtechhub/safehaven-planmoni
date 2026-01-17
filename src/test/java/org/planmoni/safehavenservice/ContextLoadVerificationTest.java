package org.planmoni.safehavenservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.planmoni.safehavenservice.mapper.SafeHavenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application Context Loading Verification Test.
 * 
 * Production Considerations:
 * - Verifies ApplicationContext can load without errors
 * - Verifies all required beans are available
 * - Verifies ObjectMapper is properly configured
 * - Verifies MapStruct mappers are generated
 * - Fails build if context fails to load
 * 
 * This test is critical for CI/CD - it ensures the application can start
 * and all beans are properly configured.
 */
@SpringBootTest(properties = {
    "spring.docker.compose.enabled=false",  // Disable Docker Compose for tests
    "spring.datasource.url=jdbc:h2:mem:testdb",  // Use H2 for tests (if needed)
    "spring.jpa.hibernate.ddl-auto=none",  // Disable Hibernate DDL
    "spring.flyway.enabled=false"  // Disable Flyway for tests
})
class ContextLoadVerificationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SafeHavenMapper safeHavenMapper;

    @Test
    void contextLoads() {
        // This test passes if ApplicationContext loads successfully
        // If context fails to load, this test will fail before running
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void objectMapperIsConfigured() {
        assertThat(objectMapper).isNotNull();
        
        // Verify JavaTimeModule is registered by attempting to serialize LocalDateTime
        LocalDateTime now = LocalDateTime.now();
        try {
            String json = objectMapper.writeValueAsString(now);
            // If JavaTimeModule is not registered, this would throw an exception
            assertThat(json).isNotNull();
            assertThat(json).doesNotMatch("\\d+"); // Should not be a numeric timestamp
        } catch (Exception e) {
            throw new AssertionError("ObjectMapper does not have JavaTimeModule configured", e);
        }
    }

    @Test
    void mapStructMapperIsAvailable() {
        assertThat(safeHavenMapper).isNotNull();
        
        // Verify it's the generated implementation
        String className = safeHavenMapper.getClass().getName();
        assertThat(className).isEqualTo("org.planmoni.safehavenservice.mapper.SafeHavenMapperImpl");
    }

    @Test
    void requiredBeansExist() {
        // Verify critical beans are available
        assertThat(applicationContext.getBean(ObjectMapper.class)).isNotNull();
        assertThat(applicationContext.getBean(SafeHavenMapper.class)).isNotNull();
        
        // Verify beans are properly configured (not null or empty)
        ObjectMapper mapper = applicationContext.getBean(ObjectMapper.class);
        assertThat(mapper).isNotNull();
        
        SafeHavenMapper safeHavenMapper = applicationContext.getBean(SafeHavenMapper.class);
        assertThat(safeHavenMapper).isNotNull();
    }
}
