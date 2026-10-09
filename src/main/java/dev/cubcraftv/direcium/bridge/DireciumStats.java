package dev.cubcraftv.direcium.bridge;

/**
 * Cheap per-interval counters for the Phase D comparison runs (enable with
 * {@code -Ddirecium.stats=true}). Touched from the render thread only, so no synchronisation.
 * Free of Minecraft types so it can be unit tested.
 */
public final class DireciumStats {
	private final long intervalMillis;
	private long windowStart = Long.MIN_VALUE;
	private long passes;
	private long regions;
	private long draws;
	private long maxDrawsInRegion;

	public DireciumStats(long intervalMillis) {
		if (intervalMillis <= 0) {
			throw new IllegalArgumentException("intervalMillis must be positive, got " + intervalMillis);
		}
		this.intervalMillis = intervalMillis;
	}

	public void pass() {
		this.passes++;
	}

	public void region() {
		this.regions++;
	}

	public void draws(int count) {
		this.draws += count;
		this.maxDrawsInRegion = Math.max(this.maxDrawsInRegion, count);
	}

	/** Returns a summary line when the interval has elapsed (and resets), else {@code null}. */
	public String poll(long nowMillis) {
		if (this.windowStart == Long.MIN_VALUE) {
			this.windowStart = nowMillis;
			return null;
		}
		long elapsed = nowMillis - this.windowStart;
		if (elapsed < this.intervalMillis) {
			return null;
		}
		String line = String.format(
			"%.1fs: %d passes, %d region updates, %d sub-draws (max %d in one region)",
			elapsed / 1000.0, this.passes, this.regions, this.draws, this.maxDrawsInRegion
		);
		this.windowStart = nowMillis;
		this.passes = 0;
		this.regions = 0;
		this.draws = 0;
		this.maxDrawsInRegion = 0;
		return line;
	}
}
