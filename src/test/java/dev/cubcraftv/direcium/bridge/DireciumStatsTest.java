package dev.cubcraftv.direcium.bridge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DireciumStatsTest {
	@Test
	void reportsOnlyAfterTheIntervalAndThenResets() {
		DireciumStats stats = new DireciumStats(5000);
		assertNull(stats.poll(1000));
		stats.pass();
		stats.region();
		stats.draws(12);
		stats.draws(30);
		assertNull(stats.poll(5999));

		String line = stats.poll(6000);
		assertNotNull(line);
		assertTrue(line.contains("1 passes"), line);
		assertTrue(line.contains("1 region updates"), line);
		assertTrue(line.contains("42 sub-draws"), line);
		assertTrue(line.contains("max 30"), line);

		assertNull(stats.poll(6001));
		stats.pass();
		String next = stats.poll(11_000);
		assertNotNull(next);
		assertTrue(next.contains("0 sub-draws"), next);
	}

	@Test
	void rejectsNonPositiveInterval() {
		assertThrows(IllegalArgumentException.class, () -> new DireciumStats(0));
	}
}
