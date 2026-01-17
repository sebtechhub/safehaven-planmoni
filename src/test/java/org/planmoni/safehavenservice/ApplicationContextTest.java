package org.planmoni.safehavenservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.planmoni.safehavenservice.mapper.SafeHavenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application Context Integration Test.
 * 
 * Production Considerations:
 * - Verifies ApplicationContext can load without errors
 * - Verifies critical beans are available for dependency injection
 * - Fails build if bean wiring fails
 * 
 * This test runs during CI/CD to ensure:
 * - No circular dependencies
 * - All required beans are configured
 * - MapStruct mappers are generated and available
 * - ObjectMapper is properly configured
 */
@SpringBootTest
class ApplicationContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Test passes if ApplicationContext loads successfully
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void objectMapperBeanExists() {
        // Verify ObjectMapper bean is available
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        assertThat(objectMapper).isNotNull();
        assertThat(objectMapper).isInstanceOf(ObjectMapper.class);
    }

    @Test
    void safeHavenMapperBeanExists() {
        // Verify MapStruct mapper is generated and available as Spring bean
        SafeHavenMapper mapper = applicationContext.getBean(SafeHavenMapper.class);
        assertThat(mapper).isNotNull();
        assertThat(mapper).isInstanceOf(SafeHavenMapper.class);
        
        // Verify implementation class exists (MapStruct generated)
        String className = mapper.getClass().getName();
        assertThat(className).isEqualTo("org.planmoni.safehavenservice.mapper.SafeHavenMapperImpl");
    }

    @Test
    void requiredBeansAreAvailable() {
        // Verify critical beans are available
        // Note: ObjectMapper is auto-configured by Spring Boot, bean name may vary
        assertThat(applicationContext.getBean(ObjectMapper.class)).isNotNull();
        assertThat(applicationContext.getBean("safeHavenMapper")).isNotNull();
    }

    @Test
    void objectMapperHasJavaTimeModule() {
        // Verify ObjectMapper is configured with JavaTimeModule for LocalDateTime support
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        assertThat(objectMapper).isNotNull();
        
        // Verify JavaTimeModule is registered (Spring Boot auto-configures this)
        // by attempting to serialize a LocalDateTime
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        try {
            String json = objectMapper.writeValueAsString(now);
            // If JavaTimeModule is not registered, this would throw an exception
            assertThat(json).isNotNull();
        } catch (Exception e) {
            throw new AssertionError("ObjectMapper does not have JavaTimeModule configured", e);
        }
    }
}
