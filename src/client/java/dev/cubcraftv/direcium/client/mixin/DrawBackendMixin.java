package dev.cubcraftv.direcium.client.mixin;

import dev.cubcraftv.direcium.Direcium;
import dev.cubcraftv.direcium.client.bridge.DirectExpBackend;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium picks its terrain draw path in {@code DrawBackend.chooseBackend()}: a Vulkan variant only when the
 * device backend is a {@code VulkanDevice}, otherwise {@code OPENGL}, whose batch calls a multi-draw overload
 * DirectExp does not implement.
 *
 * <p>Sodium's {@code VK_MULTIDRAW} path is backend-neutral: its batch packs {@code [firstIndex, indexCount,
 * baseVertex]} triples (index units) for {@code RenderPass.multiDrawIndexed(IntBuffer, ...)}, and the per-region
 * constants go through {@code RenderPass.pushConstants}. DirectExp implements both, so selecting that variant
 * is the whole bridge.
 *
 * <p>The method runs from {@code DrawBackend}'s static initialiser, so the answer is final for the session.
 */
@Mixin(DrawBackend.class)
public abstract class DrawBackendMixin {
	@Inject(method = "chooseBackend", at = @At("HEAD"), cancellable = true)
	private static void direcium$chooseForDirectExp(CallbackInfoReturnable<DrawBackend> cir) {
		if (DirectExpBackend.isActive()) {
			Direcium.LOGGER.info("Direcium: DirectExp is the active backend; Sodium terrain uses its VK_MULTIDRAW path.");
			cir.setReturnValue(DrawBackend.VK_MULTIDRAW);
		}
	}
}
