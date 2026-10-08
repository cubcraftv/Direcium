package dev.cubcraftv.direcium.client.bridge;

import dev.cubcraftv.direcium.bridge.DireciumDrawParameters;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;

/**
 * One region's terrain draws, packed for {@code RenderPass.multiDrawIndexed(IntBuffer, ...)}.
 * DirectExp executes that as a loop of indexed draws. Needs the device to report
 * {@code multiDrawDirectInterleaved} with a draw-count limit of at least the batch
 * capacity (DirectExp plan step D1).
 */
public final class DireciumMultiDrawBatch extends MultiDrawBatch {
	private final DireciumDrawParameters parameters;

	public DireciumMultiDrawBatch(int capacity) {
		this.parameters = new DireciumDrawParameters(capacity);
	}

	@Override
	public void put(int size, int elementCount, int baseVertex, long elementOffset) {
		this.parameters.put(size, elementCount, baseVertex, elementOffset);
		this.updateMaxElementCount(elementCount);
	}

	@Override
	public void draw(DrawContext context) {
		if (this.size <= 0) {
			return;
		}
		context.getPass().multiDrawIndexed(this.parameters.view(this.size), 1, 0, this.size);
	}

	@Override
	public void delete() {
		// Direct buffer memory is released by the GC with the batch.
	}
}
