package com.cykreet.arch.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigManager extends Manager {
	private static FileConfiguration config;
	private static boolean papiSupport;
	private static OfflinePlayer papiPlayer;
	private static boolean authenticationEnabled;
	
	public void setup(FileConfiguration file) {
		config = file;
		this.reload();
	}

	public void reload() {
		authenticationEnabled = true;
		Plugin placeholderAPIPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
		papiSupport = placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled();
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public boolean getPlaceholderAPISupport() {
		return papiSupport;
	}

	public void setPapiPlayer(OfflinePlayer player) {
		papiPlayer = player;
	}

	public OfflinePlayer getPapiPlayer() {
		return papiPlayer;
	}

	public boolean getAuthenticationEnabled() {
		return authenticationEnabled;
	}

	public void setAuthenticationEnabled(boolean enable) {
		authenticationEnabled = enable;
		return;
	}
}
