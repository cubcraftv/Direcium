package dev.cubcraftv.direcium.client.mixin;

import dev.cubcraftv.direcium.client.bridge.DirectExpBackend;
import dev.cubcraftv.direcium.client.bridge.DireciumMultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Same reason as {@link DrawContextMixin}: the batch factory also switches on the backend enum. */
@Mixin(MultiDrawBatch.class)
public abstract class MultiDrawBatchMixin {
	@Inject(method = "newBatch", at = @At("HEAD"), cancellable = true)
	private static void direcium$newBatchForDirectExp(int capacity, CallbackInfoReturnable<MultiDrawBatch> cir) {
		if (DirectExpBackend.isActive()) {
			cir.setReturnValue(new DireciumMultiDrawBatch(capacity));
		}
	}
}
