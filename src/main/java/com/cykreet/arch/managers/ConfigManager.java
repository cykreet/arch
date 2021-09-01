package com.cykreet.arch.managers;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ConfigManager extends Manager {
	private static FileConfiguration config;
	private static boolean papiSupport;
	private static OfflinePlayer papiPlayer;
	
	public void load(FileConfiguration file) {
		ConfigManager.config = file;
		Plugin placeholderAPIPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
		papiSupport = placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled();
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public boolean getPlaceholderAPISupport() {
		return papiSupport;
	}

	public void setPapiPlayer(@NotNull OfflinePlayer player) {
		papiPlayer = player;
	}

	@Nullable
	public OfflinePlayer getPapiPlayer() {
		return papiPlayer;
	}
}
