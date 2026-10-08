package dev.cubcraftv.direcium.client.mixin;

import dev.cubcraftv.direcium.client.bridge.DirectExpBackend;
import dev.cubcraftv.direcium.client.bridge.DireciumDrawContext;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium picks its draw context with a {@code switch} over an enum that has no
 * DirectX entry (it would fall through to OpenGL), so intercept the factory.
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
	@Inject(method = "create", at = @At("HEAD"), cancellable = true)
	private static void direcium$createForDirectExp(CallbackInfoReturnable<DrawContext> cir) {
		if (DirectExpBackend.isActive()) {
			cir.setReturnValue(new DireciumDrawContext());
		}
	}
}
