package dev.cubcraftv.direcium.client;

import dev.cubcraftv.direcium.Direcium;
import dev.cubcraftv.directexp.client.settings.DirectExpGraphicsApiSelectionState;
import dev.cubcraftv.directexp.config.DirectExpConfig;
import dev.cubcraftv.directexp.config.DirectExpConfigStore;
import dev.cubcraftv.directexp.config.GraphicsApiPreference;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Puts DirectExp's "Graphics API" choice on Sodium's options screen.
 *
 * <p>DirectExp adds that option to vanilla's {@code VideoSettingsScreen}, but Sodium replaces the video settings
 * screen, so with Sodium installed the option would be unreachable. This entry point registers the same
 * setting (same config file, same translation keys) as a Sodium page.
 */
public final class DireciumConfig implements ConfigEntryPoint {
	private static final String CONFIG_FILE = "directexp.properties";

	private GraphicsApiPreference selected = GraphicsApiPreference.DIRECTX;

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
		this.selected = GraphicsApiPreference.fromBackendKind(new DirectExpConfigStore(path).load().backend());
		DirectExpGraphicsApiSelectionState.onVideoSettingsOpened(this.selected.toBackendKind());

		builder.registerOwnModOptions()
			.setName("DirectExp")
			.addPage(builder.createOptionPage()
				.setName(Component.literal("DirectExp"))
				.addOptionGroup(builder.createOptionGroup()
					.addOption(builder.createEnumOption(Direcium.id("graphics_api"), GraphicsApiPreference.class)
						.setName(Component.translatable("directexp.options.graphicsApi"))
						.setTooltip(Component.translatable("directexp.options.graphicsApi.tooltip"))
						.setElementNameProvider(api -> Component.translatable("directexp.options.graphicsApi." + api.id()))
						.setDefaultValue(GraphicsApiPreference.DIRECTX)
						.setFlags(OptionFlag.REQUIRES_GAME_RESTART)
						.setBinding(value -> this.selected = value, () -> this.selected)
						.setStorageHandler(() -> persist(path)))));
	}

	/** Writes the selection while keeping the file's other settings as they are on disk right now. */
	private void persist(Path path) {
		DirectExpConfigStore store = new DirectExpConfigStore(path);
		DirectExpConfig current = store.load();
		DirectExpConfig updated = new DirectExpConfig(this.selected.toBackendKind(), current.failureMode(), current.diagnosticsEnabled());
		DirectExpGraphicsApiSelectionState.onSelectionApplied(updated.backend());
		try {
			store.save(updated);
		} catch (IOException e) {
			Direcium.LOGGER.warn("Direcium: failed to save DirectExp graphics API selection to {}", path, e);
		}
	}
}
