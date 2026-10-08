package dev.cubcraftv.direcium.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DirectExpBackendNameTest {
	@Test
	void matchesDirectExpsReportedName() {
		assertTrue(DirectExpBackendName.matches("Direct3D 11 (feature level 10.1 floor)"));
	}

	@Test
	void doesNotMatchOtherBackends() {
		assertFalse(DirectExpBackendName.matches("OpenGL 4.6"));
		assertFalse(DirectExpBackendName.matches("Vulkan 1.3"));
		assertFalse(DirectExpBackendName.matches(""));
		assertFalse(DirectExpBackendName.matches(null));
	}
}
