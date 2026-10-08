package dev.cubcraftv.direcium.client;

import dev.cubcraftv.direcium.Direcium;
import net.fabricmc.api.ClientModInitializer;

import java.lang.reflect.Method;

public class DireciumClient implements ClientModInitializer {
	private static final String REGION_CLASS = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion";

	@Override
	public void onInitializeClient() {
		// The device may not exist yet here, so only report. The mixins decide per call whether
		// DirectExp is the active backend (see DirectExpBackend).
		Direcium.LOGGER.info("Direcium loaded: Sodium's terrain path switches to DirectExp when its Direct3D 11 backend is active.");
		checkSodiumSurface();
	}

	/**
	 * DireciumDrawContext reads these two RenderRegion methods on every draw. Check them once at startup
	 * so a Sodium update that renames them gives a readable message instead of a NoSuchMethodError mid-frame.
	 */
	private static void checkSodiumSurface() {
		try {
			Class<?> region = Class.forName(REGION_CLASS);
			for (String name : new String[] {"getCreationTime", "getId", "getOriginX", "getOriginY", "getOriginZ"}) {
				Method method = region.getMethod(name);
				Direcium.LOGGER.debug("Direcium: found {}#{} -> {}", REGION_CLASS, name, method.getReturnType().getSimpleName());
			}
		} catch (ReflectiveOperationException | LinkageError e) {
			Direcium.LOGGER.error(
				"Direcium: Sodium's RenderRegion no longer matches what the bridge expects ({}). "
					+ "This Sodium build is probably newer than the tested range; terrain drawing on DirectExp will fail.",
				e.toString()
			);
		}
	}
}
