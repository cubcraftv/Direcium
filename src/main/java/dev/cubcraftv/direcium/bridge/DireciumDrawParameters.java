package dev.cubcraftv.direcium.bridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * The {@code IntBuffer} handed to {@code RenderPass.multiDrawIndexed(IntBuffer, ...)}:
 * three ints per draw, {@code firstIndex, indexCount, baseVertex}. This is the
 * interleaved layout Blaze3D defines for {@code multiDrawDirectInterleaved}
 * (the same as Vulkan's {@code VkMultiDrawIndexedInfoEXT}), and the order
 * DirectExp's {@code multiDrawIndexed} reads.
 *
 * <p>Kept free of Minecraft types so it can be unit tested on its own.
 */
public final class DireciumDrawParameters {
	public static final int INTS_PER_DRAW = 3;

	private final int capacity;
	private final IntBuffer storage;
	private final IntBuffer view;

	public DireciumDrawParameters(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive, got " + capacity);
		}
		this.capacity = capacity;
		this.storage = ByteBuffer.allocateDirect(capacity * INTS_PER_DRAW * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
		this.view = this.storage.duplicate();
	}

	public int capacity() {
		return this.capacity;
	}

	/**
	 * Stores draw {@code index}. {@code elementOffset} is in <em>indices</em>
	 * (Sodium passes it that way; only its OpenGL batch converts it to bytes).
	 */
	public void put(int index, int elementCount, int baseVertex, long elementOffset) {
		if (index < 0 || index >= this.capacity) {
			throw new IndexOutOfBoundsException("draw " + index + " is outside capacity " + this.capacity);
		}
		int firstIndex = Math.toIntExact(elementOffset);
		int base = index * INTS_PER_DRAW;
		this.storage.put(base, firstIndex);
		this.storage.put(base + 1, elementCount);
		this.storage.put(base + 2, baseVertex);
	}

	/**
	 * A view covering the first {@code drawCount} draws (position 0, limit
	 * {@code drawCount * 3}). The same object is reused on every call, which is
	 * safe because the draw is consumed before the next one is built.
	 */
	public IntBuffer view(int drawCount) {
		if (drawCount < 0 || drawCount > this.capacity) {
			throw new IllegalArgumentException("drawCount " + drawCount + " is outside capacity " + this.capacity);
		}
		this.view.clear();
		this.view.limit(drawCount * INTS_PER_DRAW);
		return this.view;
	}
}
