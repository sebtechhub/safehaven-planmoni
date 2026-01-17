package org.planmoni.safehavenservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jackson Configuration Test.
 * 
 * Production Considerations:
 * - Verifies ObjectMapper is properly configured via Jackson2ObjectMapperBuilderCustomizer
 * - Verifies JavaTimeModule is configured (LocalDateTime support)
 * - Verifies ISO-8601 date serialization (no timestamps)
 * - Verifies FAIL_ON_EMPTY_BEANS is disabled
 * 
 * This test ensures Jackson configuration is correct and prevents
 * issues like @Autowired on @Bean methods.
 */
@SpringBootTest(properties = {
    "spring.docker.compose.enabled=false"  // Disable Docker Compose for tests
})
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void objectMapperIsAvailable() {
        assertThat(objectMapper).isNotNull();
    }

    @Test
    void objectMapperHasJavaTimeModule() {
        // Verify LocalDateTime can be serialized (JavaTimeModule required)
        LocalDateTime now = LocalDateTime.now();
        
        try {
            String json = objectMapper.writeValueAsString(now);
            // If JavaTimeModule is not registered, this would throw an exception
            assertThat(json).isNotNull();
            // Verify ISO-8601 format (not timestamp)
            assertThat(json).doesNotMatch("\\d+"); // Should not be a numeric timestamp
        } catch (Exception e) {
            throw new AssertionError("ObjectMapper does not have JavaTimeModule configured", e);
        }
    }

    @Test
    void objectMapperSerializesDatesAsIso8601() {
        // Verify dates are serialized as ISO-8601 strings, not timestamps
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        
        try {
            String json = objectMapper.writeValueAsString(dateTime);
            // Should be ISO-8601 format like "2024-01-15T10:30:00"
            assertThat(json).contains("2024-01-15");
            assertThat(json).contains("T");
            assertThat(json).contains("10:30");
            // Should NOT be a numeric timestamp
            assertThat(json).doesNotMatch("^\\d+$");
        } catch (Exception e) {
            throw new AssertionError("ObjectMapper does not serialize dates as ISO-8601", e);
        }
    }

    @Test
    void objectMapperDoesNotFailOnEmptyBeans() {
        // Verify FAIL_ON_EMPTY_BEANS is disabled
        EmptyBean emptyBean = new EmptyBean();
        
        try {
            String json = objectMapper.writeValueAsString(emptyBean);
            // Should serialize without error (even if empty)
            assertThat(json).isNotNull();
        } catch (Exception e) {
            throw new AssertionError("ObjectMapper fails on empty beans (FAIL_ON_EMPTY_BEANS should be disabled)", e);
        }
    }

    @Test
    void objectMapperConfigurationIsCorrect() {
        // Verify configuration features are set correctly
        assertThat(objectMapper.getSerializationConfig()
                .isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                .isFalse(); // Should be false (ISO-8601 format)
        
        assertThat(objectMapper.getSerializationConfig()
                .isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS))
                .isFalse(); // Should be false (customizer disables this)
    }

    /**
     * Empty bean class for testing FAIL_ON_EMPTY_BEANS configuration.
     */
    private static class EmptyBean {
        // Intentionally empty for testing
    }
}
