package dev.cubcraftv.direcium.bridge;

/** The name-matching rule behind {@code DirectExpBackend}, free of Minecraft types so it can be unit tested. */
public final class DirectExpBackendName {
	/** DirectExp reports "Direct3D 11 (feature level 10.1 floor)". */
	public static final String PREFIX = "Direct3D 11";

	private DirectExpBackendName() {
	}

	public static boolean matches(String backendName) {
		return backendName != null && backendName.startsWith(PREFIX);
	}
}
