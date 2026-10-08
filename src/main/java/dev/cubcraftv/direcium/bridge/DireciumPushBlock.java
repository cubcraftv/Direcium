package dev.cubcraftv.direcium.bridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Byte layout of the per-region block Sodium's terrain shader declares as
 * {@code layout(push_constant) uniform { vec3 u_RegionOffset; int u_CurrentTime; uint u_RegionID; }}.
 * DirectExp rewrites that block to a std140 constant buffer, for which this
 * layout is identical: three floats at 0/4/8, the time at 12, the region id at 16.
 *
 * <p>Kept free of Minecraft types so it can be unit tested on its own.
 */
public final class DireciumPushBlock {
	/** Matches {@code DrawContext.PUSH_CONSTANT_RANGE} in Sodium. */
	public static final int SIZE_BYTES = 20;

	private DireciumPushBlock() {
	}

	/** A buffer sized and ordered for {@link #encode}. */
	public static ByteBuffer allocate() {
		return ByteBuffer.allocate(SIZE_BYTES).order(ByteOrder.nativeOrder());
	}

	/**
	 * Writes the block at offset 0 and leaves {@code out} ready to read
	 * (position 0, limit {@link #SIZE_BYTES}).
	 */
	public static void encode(ByteBuffer out, float regionX, float regionY, float regionZ, int timeMillis, int regionId) {
		if (out.capacity() < SIZE_BYTES) {
			throw new IllegalArgumentException("Push block buffer needs " + SIZE_BYTES + " bytes, has " + out.capacity());
		}
		if (out.order() != ByteOrder.nativeOrder()) {
			// The shader reads native-endian values; DirectExp copies the bytes as they are.
			throw new IllegalArgumentException("Push block buffer must use native byte order");
		}

		out.clear();
		out.putFloat(0, regionX);
		out.putFloat(4, regionY);
		out.putFloat(8, regionZ);
		out.putInt(12, timeMillis);
		out.putInt(16, regionId);
		out.limit(SIZE_BYTES);
		out.position(0);
	}
}
