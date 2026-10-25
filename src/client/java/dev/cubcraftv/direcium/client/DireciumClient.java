package dev.cubcraftv.direcium.client;

import dev.cubcraftv.direcium.Direcium;
import net.fabricmc.api.ClientModInitializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DireciumClient implements ClientModInitializer {
	private static final String DRAW_BACKEND_CLASS = "net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend";
	private static final String TARGET_CONSTANT = "VK_MULTIDRAW";
	private static final String TARGET_METHOD = "chooseBackend";

	@Override
	public void onInitializeClient() {
		// The device may not exist yet here, so only report. DrawBackendMixin decides when Sodium selects its backend.
		Direcium.LOGGER.info("Direcium loaded: Sodium's terrain path switches to DirectExp when its Direct3D 11 backend is active.");
		checkSodiumSurface();
	}

	/**
	 * DrawBackendMixin needs Sodium's {@code DrawBackend.chooseBackend()} and the {@code VK_MULTIDRAW} constant.
	 * Check both once at startup so a Sodium update that renames them gives a readable message.
	 *
	 * <p>The class is loaded without initialising it: {@code DrawBackend}'s static initialiser needs the GPU device,
	 * which does not exist yet.
	 */
	private static void checkSodiumSurface() {
		try {
			Class<?> backend = Class.forName(DRAW_BACKEND_CLASS, false, DireciumClient.class.getClassLoader());

			// Field reflection only: getEnumConstants() would run the static initialiser.
			boolean hasConstant = false;
			for (Field field : backend.getDeclaredFields()) {
				if (field.isEnumConstant() && TARGET_CONSTANT.equals(field.getName())) {
					hasConstant = true;
					break;
				}
			}
			boolean hasMethod = false;
			for (Method method : backend.getDeclaredMethods()) {
				if (TARGET_METHOD.equals(method.getName())) {
					hasMethod = true;
					break;
				}
			}

			if (!hasConstant || !hasMethod) {
				Direcium.LOGGER.error(
					"Direcium: Sodium's DrawBackend no longer has {} / {}(). This Sodium build is probably newer "
						+ "than the tested range; terrain drawing on DirectExp will not work.",
					TARGET_CONSTANT, TARGET_METHOD
				);
			}
		} catch (ReflectiveOperationException | LinkageError e) {
			Direcium.LOGGER.error("Direcium: could not inspect Sodium's DrawBackend ({}).", e.toString());
		}
	}
}
