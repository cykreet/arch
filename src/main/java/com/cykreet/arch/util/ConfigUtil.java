package com.cykreet.arch.util;

import java.util.Map;
import java.util.Map.Entry;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.ConfigManager;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class ConfigUtil {
	private static ConfigManager configManager = Arch.getManager(ConfigManager.class);
	private static FileConfiguration config = configManager.getConfig();

	public static boolean contains(@NotNull ConfigPath path) {
		String configPath = path.label;
		return config.contains(configPath);
	}

	public static int getInt(@NotNull ConfigPath path) {
		String configPath = path.label;
		return config.getInt(configPath);
	}

	public static String getString(@NotNull ConfigPath path) {
		return getString(path, null, null);
	}

	public static String getString(@NotNull ConfigPath path, @NotNull Map<String, String> placeholders) {
		return getString(path, placeholders);
	}

	public static String getString(@NotNull ConfigPath path, @NotNull OfflinePlayer player) {
		return getString(path, null, player);
	}

	public static String getString(@NotNull ConfigPath path, @Nullable Map<String, String> placeholders, @Nullable OfflinePlayer player) {
		String configPath = path.label;
		String configString = config.getString(configPath);
		boolean isOptional = configPath.endsWith("?");
		if (configString == null) {
			if (isOptional) return null;
			String message = String.format(Message.INTERNAL_REQUIRED_CONFIG_PATH.content, configPath);
			LoggerUtil.errorAndExit(message);
			return null;
		}

		String formatted = formatPlaceholders(configString, placeholders, player);
		// todo: only translate on minecraft message
		return ChatColor.translateAlternateColorCodes('&', formatted);
	}

	private static String formatPlaceholders(@NotNull String input, @Nullable Map<String, String> placeholders, @Nullable OfflinePlayer player) {
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
		if (!configManager.getPlaceholderAPISupport() || PlaceholderAPI.containsPlaceholders(output)) return output;
		OfflinePlayer placeholderPlayer = player != null ? player : configManager.getPapiPlayer();
		return PlaceholderAPI.setPlaceholders(placeholderPlayer, output);
	}
}
