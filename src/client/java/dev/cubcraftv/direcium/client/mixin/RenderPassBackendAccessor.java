package dev.cubcraftv.direcium.client.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Our own accessor (instead of Sodium's internal one) for the backend behind a {@link RenderPass}. */
@Mixin(RenderPass.class)
public interface RenderPassBackendAccessor {
	@Accessor("backend")
	RenderPassBackend direcium$getBackend();
}
