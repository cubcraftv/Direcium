package dev.cubcraftv.direcium.client.bridge;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.cubcraftv.direcium.bridge.DirectExpBackendName;

/**
 * Tells whether the active Blaze3D device is DirectExp's. Uses only Mojang's
 * public {@code DeviceInfo.backendName()} (the same kind of check Sodium makes
 * for OpenGL), so detection needs no compile-time knowledge of DirectExp's internals.
 */
public final class DirectExpBackend {
	private static volatile Boolean active;

	private DirectExpBackend() {
	}

	public static boolean isActive() {
		Boolean cached = active;
		if (cached != null) {
			return cached;
		}

		GpuDevice device = RenderSystem.getDevice();
		if (device == null) {
			// Not created yet: answer "no" without caching, so a later call can still say yes.
			return false;
		}

		boolean result = DirectExpBackendName.matches(device.getDeviceInfo().backendName());
		active = result;
		return result;
	}
}
