package org.planmoni.safehavenservice;

import org.junit.jupiter.api.Test;
import org.planmoni.safehavenservice.mapper.SafeHavenMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MapStruct Generation Verification Test.
 * 
 * Production Considerations:
 * - Verifies MapStruct implementation class exists at compile-time
 * - Fails build if annotation processing failed
 * - Ensures mapper implementations are generated before tests run
 * 
 * This test ensures MapStruct annotation processing succeeded
 * and the generated implementation is available on the classpath.
 */
class MapStructGenerationTest {

    @Test
    void safeHavenMapperImplementationExists() {
        // This test will fail if MapStruct implementation was not generated
        // The implementation class should exist at: target/generated-sources/annotations/...
        SafeHavenMapper mapper = org.mapstruct.factory.Mappers.getMapper(SafeHavenMapper.class);
        
        assertThat(mapper).isNotNull();
        assertThat(mapper.getClass().getName())
                .isEqualTo("org.planmoni.safehavenservice.mapper.SafeHavenMapperImpl");
        
        // Verify it's actually the generated implementation, not an interface
        assertThat(mapper.getClass().isInterface()).isFalse();
    }
}
