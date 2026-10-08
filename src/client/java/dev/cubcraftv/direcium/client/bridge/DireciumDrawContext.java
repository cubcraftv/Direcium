package dev.cubcraftv.direcium.client.bridge;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import dev.cubcraftv.direcium.bridge.DireciumPushBlock;
import dev.cubcraftv.direcium.client.mixin.RenderPassBackendAccessor;
import dev.cubcraftv.directexp.client.api.DxPushConstants;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

import java.nio.ByteBuffer;

/**
 * Sodium's per-pass draw state for DirectExp. Sodium calls {@link #setContext}
 * once per terrain pass and {@link #updateData} before each region's batch; here
 * that becomes one {@link DxPushConstants#setPushConstants} per region, which
 * DirectExp feeds to the shader's push-constant block on every draw.
 */
public final class DireciumDrawContext extends DrawContext {
	private final ByteBuffer block = DireciumPushBlock.allocate();
	private DxPushConstants target;

	@Override
	public void setContext(RenderPass pass, RenderPipeline pipeline) {
		this.pass = pass;

		RenderPassBackend backend = ((RenderPassBackendAccessor) pass).direcium$getBackend();
		if (!(backend instanceof DxPushConstants pushConstants)) {
			throw new IllegalStateException(
				"Direcium expected DirectExp's render pass backend (" + DxPushConstants.class.getName()
					+ ") but got " + backend.getClass().getName()
			);
		}
		this.target = pushConstants;
	}

	@Override
	public void updateData(RenderRegion region, CameraTransform camera) {
		float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
		float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
		float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);
		int time = Math.toIntExact(System.currentTimeMillis() - region.getCreationTime());

		DireciumPushBlock.encode(this.block, x, y, z, time, region.getId());
		this.target.setPushConstants(this.block);
	}

	@Override
	public void rotate() {
	}

	@Override
	public void delete() {
	}

	@Override
	public void endDraw() {
	}
}
