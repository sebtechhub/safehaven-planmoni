package org.planmoni.safehavenservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic Application Context Test.
 * 
 * This test verifies that the Spring Boot application context can load
 * without errors. It's a minimal smoke test to ensure basic configuration
 * is correct.
 * 
 * For more comprehensive tests, see:
 * - ApplicationContextTest: Verifies all required beans are available
 * - MapStructGenerationTest: Verifies MapStruct implementations are generated
 */
@SpringBootTest(properties = {
    "spring.docker.compose.enabled=false"  // Disable Docker Compose for tests
})
class SafehavenServiceApplicationTests {

	@Test
	void contextLoads() {
		// Test passes if ApplicationContext loads successfully
		// This is a minimal smoke test
	}

}
