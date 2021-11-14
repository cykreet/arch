package com.cykreet.arch.util;

import java.util.Map;
import java.util.Map.Entry;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.ConfigManager;
import com.cykreet.arch.util.enums.ConfigPath;
import com.cykreet.arch.util.enums.Message;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class ConfigUtil {
	private static final ConfigManager CONFIG_MANAGER = Arch.getManager(ConfigManager.class);
	private static final FileConfiguration CONFIG = CONFIG_MANAGER.getConfig();

	public static boolean contains(@NotNull final ConfigPath path) {
		String configPath = path.label;
		return CONFIG.contains(configPath);
	}

	public static int getInt(@NotNull final ConfigPath path) {
		String configPath = path.label;
		return CONFIG.getInt(configPath);
	}

	public static String getString(@NotNull final ConfigPath path) {
		return ConfigUtil.getString(path, null, null);
	}

	public static String getString(
		@NotNull final ConfigPath path,
		@NotNull final Map<String, String> placeholders
	) {
		return ConfigUtil.getString(path, placeholders);
	}

	public static String getString(@NotNull final ConfigPath path, @NotNull final OfflinePlayer player) {
		return ConfigUtil.getString(path, null, player);
	}

	public static String getString(
		@NotNull final ConfigPath path,
		@Nullable final Map<String, String> placeholders,
		@Nullable final OfflinePlayer player
	) {
		String configPath = path.label;
		String configString = CONFIG.getString(configPath, null);
		if (configString == null) {
			boolean isOptional = configPath.endsWith("?");
			if (isOptional) return null;
			String message = String.format(
				"Required config path \"%s\" is invalid or missing.",
				configPath
			);

			LoggerUtil.errorAndExit(message);
			return null;
		}

		String formatted = ConfigUtil.formatPlaceholders(configString, placeholders, player);
		// todo: only translate on minecraft message
		return ChatColor.translateAlternateColorCodes('&', formatted);
	}

	public static String formatMessage(final Message message, final Object ...args) {
		return String.format(message.content, args);
	}

	private static String formatPlaceholders(
		@NotNull final String input,
		@Nullable final Map<String, String> placeholders,
		@Nullable final OfflinePlayer player
	) {
		StringBuilder stringBuilder = new StringBuilder(input);
		if (placeholders != null) {
			for (Entry<String, String> entry : placeholders.entrySet()) {
				String key = "{{" + entry.getKey() + "}}";
				String value = entry.getValue();
				int start = stringBuilder.indexOf(key, 0);
				while (start > -1) {
					int end = start + key.length();
					int nextSearchStart = start + value.length();
					stringBuilder.replace(start, end, value);
					start = stringBuilder.indexOf(key, nextSearchStart);
				}
			}
		}

		String output = stringBuilder.toString();
		if (!CONFIG_MANAGER.getPlaceholderAPISupport() || PlaceholderAPI.containsPlaceholders(output)) {
			return output;
		}
		OfflinePlayer placeholderPlayer = player != null ? player : CONFIG_MANAGER.getPapiPlayer();
		return PlaceholderAPI.setPlaceholders(placeholderPlayer, output);
	}
}
