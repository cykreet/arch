package com.cykreet.arch.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.ConfigManager;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class ConfigUtil {
	private static ConfigManager configManager = Arch.getManager(ConfigManager.class);
	private static FileConfiguration config = configManager.getConfig();

	public static boolean ensureAuthenticationEnabled() {
		ConfigPath notLinked = ConfigPath.AUTH_MESSAGE_NOT_LINKED;
		if (!ConfigUtil.contains(notLinked)) {
			String notEnabledMessage = String.format("\"%s\" config option not provided, disabling authentication...", notLinked.label);
			LoggerUtil.warning(notEnabledMessage);
			configManager.setAuthenticationEnabled(false);
			return false;
		}

		return true;
	}

	public static boolean contains(@NotNull ConfigPath path) {
		String configPath = path.label;
		return config.contains(configPath);
	}

	public static int getInt(@NotNull ConfigPath path) {
		String configPath = path.label;
		return config.getInt(configPath);
	}

	public static String getString(@NotNull ConfigPath path) {
		return getString(path, null);
	}

	public static String getString(@NotNull ConfigPath path, @Nullable Map<String, String> placeholders) {
		String configPath = path.label;
		String configString = config.getString(configPath);
		boolean isOptional = configPath.endsWith("?");
		if (configString == null) {
			if (isOptional) return null;
			String message = String.format("Required config path \"%s\" is invalid or missing.", configPath);
			LoggerUtil.errorAndExit(message);
			return null;
		}

		String formatted = formatPlaceholders(configString, placeholders);
		return ChatColor.translateAlternateColorCodes('&', formatted);
	}

	private static String formatPlaceholders(@NotNull String input, @Nullable Map<String, String> placeholders) {
		if (placeholders != null) {
			for (Entry<String, String> placeholder: placeholders.entrySet()) {
				String placeholderKey = "{{" + placeholder.getKey() + "}}";
				if (!input.contains(placeholderKey)) continue;
				String placeholderValue = placeholder.getValue();
				input = input.replaceAll(Pattern.quote(placeholderKey), placeholderValue);
			}
		}

		if (!configManager.getPlaceholderAPISupport() || !PlaceholderAPI.containsPlaceholders(input)) return input;
		// todo: get context player
		return PlaceholderAPI.setPlaceholders(configManager.getPapiPlayer(), input);
	}
}
