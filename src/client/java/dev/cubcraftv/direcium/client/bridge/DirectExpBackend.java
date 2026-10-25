package dev.cubcraftv.direcium.client.bridge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import dev.cubcraftv.direcium.bridge.DirectExpBackendName;

import java.lang.ref.WeakReference;

/**
 * Tells whether the active Blaze3D device is DirectExp's. Uses only Mojang's
 * public {@code DeviceInfo.backendName()} (the same kind of check Sodium makes
 * for OpenGL), so detection needs no compile-time knowledge of DirectExp's internals.
 *
 * <p>The answer is cached per device instance, not forever, so a device that is recreated
 * (backend switch, device loss recovery) is evaluated again.
 */
public final class DirectExpBackend {
	private static WeakReference<GpuDevice> cachedDevice = new WeakReference<>(null);
	private static boolean cachedResult;

	private DirectExpBackend() {
	}

	public static synchronized boolean isActive() {
		GpuDevice device = RenderSystem.getDevice();
		if (device == null) {
			// Not created yet: answer "no" without caching, so a later call can still say yes.
			return false;
		}
		if (cachedDevice.get() != device) {
			cachedResult = DirectExpBackendName.matches(device.getDeviceInfo().backendName());
			cachedDevice = new WeakReference<>(device);
		}
		return cachedResult;
	}
}
