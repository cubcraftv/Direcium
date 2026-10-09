package dev.cubcraftv.direcium.bridge;

/**
 * The {@code u_CurrentTime} value: milliseconds since the region was created, as the 32-bit int the
 * shader reads. Sodium's own contexts use {@code Math.toIntExact}, which throws once a region is
 * about 24.8 days old; this clamps instead. The value only drives the chunk fade-in, which finished
 * long before, so saturating is harmless and a long session cannot crash the render thread.
 */
public final class DireciumTime {
	private DireciumTime() {
	}

	public static int sinceCreation(long nowMillis, long creationMillis) {
		long elapsed = nowMillis - creationMillis;
		if (elapsed > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (elapsed < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) elapsed;
	}
}
