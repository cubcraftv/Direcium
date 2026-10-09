package dev.cubcraftv.direcium.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DireciumTimeTest {
	@Test
	void normalElapsedTimePassesThrough() {
		assertEquals(1500, DireciumTime.sinceCreation(11_500L, 10_000L));
	}

	@Test
	void saturatesInsteadOfThrowingAfterAboutTwentyFiveDays() {
		long created = 1_000L;
		long now = created + 30L * 24 * 60 * 60 * 1000;
		assertEquals(Integer.MAX_VALUE, DireciumTime.sinceCreation(now, created));
	}

	@Test
	void clockMovingBackwardsClampsAtTheLowEnd() {
		assertEquals(Integer.MIN_VALUE, DireciumTime.sinceCreation(0L, 1L << 40));
		assertEquals(-5, DireciumTime.sinceCreation(95L, 100L));
	}
}
