package org.planmoni.safehavenservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson/ObjectMapper Configuration for Spring Boot 4.x.
 * 
 * Production Considerations:
 * - Direct ObjectMapper bean creation (no deprecated customizers)
 * - Spring Boot 4.x compatible (works with Jackson 2 and Jackson 3)
 * - JavaTimeModule explicitly registered for LocalDateTime/LocalDate support
 * - ISO-8601 date serialization (no timestamps)
 * - Backward compatible with existing code
 * 
 * Why Not Use Jackson2ObjectMapperBuilderCustomizer:
 * - Deprecated in Spring Boot 4.x (marked for removal)
 * - Causes ClassNotFoundException in Spring Boot 4.0.1
 * - Direct bean creation is simpler and more maintainable
 * 
 * Note: This configuration works with both Jackson 2 (com.fasterxml.jackson.*)
 * and Jackson 3 (tools.jackson.*), making it future-proof.
 */
@Configuration
public class JacksonConfig {

    /**
     * Primary ObjectMapper bean for JSON serialization/deserialization.
     * 
     * Created directly without dependencies on Spring Boot builders or customizers.
     * This approach is:
     * - Simple and maintainable
     * - Spring Boot 4.x compatible
     * - Backward compatible with Jackson 2
     * - Forward compatible with Jackson 3
     * 
     * Configuration:
     * - JavaTimeModule: LocalDateTime, LocalDate, etc. support
     * - WRITE_DATES_AS_TIMESTAMPS=false: ISO-8601 date format (not numeric timestamps)
     * - FAIL_ON_EMPTY_BEANS=false: Allow serialization of empty objects
     * - PropertyNamingStrategy: camelCase (standard Java convention)
     * 
     * @return Configured ObjectMapper bean
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // Register JavaTimeModule for Java 8+ date/time types
                .registerModule(new JavaTimeModule())
                // Disable timestamp serialization (use ISO-8601 format)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Allow serialization of empty beans
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Use camelCase property naming
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }
}

