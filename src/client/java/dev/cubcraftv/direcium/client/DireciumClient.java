package dev.cubcraftv.direcium.client;

import dev.cubcraftv.direcium.Direcium;
import net.fabricmc.api.ClientModInitializer;

public class DireciumClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The device may not exist yet here, so only report. The mixins decide per call whether
		// DirectExp is the active backend (see DirectExpBackend).
		Direcium.LOGGER.info("Direcium loaded: Sodium's terrain path switches to DirectExp when its Direct3D 11 backend is active.");
	}
}