package com.cykreet.arch;

import java.util.UUID;

import com.cykreet.arch.listeners.PlayerChatListener;
import com.cykreet.arch.listeners.PlayerDeathListener;
import com.cykreet.arch.listeners.PlayerJoinListener;
import com.cykreet.arch.listeners.PlayerPreLoginListener;
import com.cykreet.arch.listeners.PlayerQuitListener;
import com.cykreet.arch.managers.CacheManager;
import com.cykreet.arch.managers.ConfigManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.managers.PersistManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Arch extends JavaPlugin {
	private static Arch INSTANCE;
	public ConfigManager configManager;
	public PersistManager database;
	public DiscordManager discord;
	public CacheManager codesCache;

	public Arch() {
		INSTANCE = this;
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.database = new PersistManager(this.getDataFolder(), "linked-users.db");
		this.configManager = new ConfigManager(this.getConfig());
		this.discord = new DiscordManager();
		int codeExpiry = ConfigUtil.getInt(ConfigPath.AUTH_CODE_EXPIRE);
		this.codesCache = new CacheManager(codeExpiry);

		this.registerListener(new PlayerPreLoginListener());
		this.registerListener(new PlayerDeathListener());
		this.registerListener(new PlayerChatListener());
		this.registerListener(new PlayerJoinListener());
		this.registerListener(new PlayerQuitListener());

		ConfigUtil.ensureAuthenticationEnabled();
		String configPlayer = ConfigUtil.getString(ConfigPath.DEFAULT_PLAYER);
		if (configPlayer != null) {
			UUID defaultPlayer = UUID.fromString(ConfigUtil.getString(ConfigPath.DEFAULT_PLAYER));
			this.configManager.setPapiPlayer(Bukkit.getOfflinePlayer(defaultPlayer));
		}

		String botToken = ConfigUtil.getString(ConfigPath.BOT_TOKEN);
		String activity = ConfigUtil.getString(ConfigPath.BOT_STATUS);
		if (botToken == null) return;
		this.discord.login(botToken, activity);
		this.database.connect();
	}

	@Override
	public void onDisable() {
		this.database.close();
		this.discord.logout();
		this.codesCache.getCache().invalidateAll();
		Bukkit.getScheduler().cancelTasks(this);
	}

	public void reloadPlugin() {
		// to be used with future reload command
		// todo: doesn't reload listeners and database and shit
		this.reloadConfig();
		this.configManager.reload();
	}

	private void registerListener(Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
	}

	public static Arch getInstance() {
		return Arch.INSTANCE;
	}
}
