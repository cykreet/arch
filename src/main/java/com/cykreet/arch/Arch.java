package com.cykreet.arch;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cykreet.arch.listeners.PlayerChatListener;
import com.cykreet.arch.listeners.PlayerGenericListener;
import com.cykreet.arch.listeners.PlayerPreLoginListener;
import com.cykreet.arch.managers.CodesManager;
import com.cykreet.arch.managers.ConfigManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.managers.Manager;
import com.cykreet.arch.managers.DatabaseManager;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.enums.ConfigPath;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Arch extends JavaPlugin {
	private static Map<Class<Manager>, Manager> managers = new HashMap<>();
	private DiscordManager discordManager;
	private ConfigManager configManager;
	private CodesManager codesManager;
	private DatabaseManager database;
	private static Arch instance;

	public Arch() {
		instance = this;
	}

	@Override
	public final void onEnable() {
		this.saveDefaultConfig();
		this.registerListeners();
		this.database = getManager(DatabaseManager.class);
		this.codesManager = getManager(CodesManager.class);
		this.configManager = getManager(ConfigManager.class);
		this.discordManager = getManager(DiscordManager.class);
		this.configManager.load(this.getConfig());
		String configPlayer = ConfigUtil.getString(ConfigPath.DEFAULT_PLAYER);
		if (configPlayer != null) {
			UUID defaultPlayer = UUID.fromString(configPlayer);
			this.configManager.setPapiPlayer(Bukkit.getOfflinePlayer(defaultPlayer));
		}

		int codeExpiry = ConfigUtil.getInt(ConfigPath.AUTH_CODE_EXPIRE);
		this.codesManager.createCache(codeExpiry);
		this.database.connect(this.getDataFolder(), "linked-users.sqlite");

		String botToken = ConfigUtil.getString(ConfigPath.BOT_TOKEN);
		// handled by config util
		if (botToken == null) return;
		String activity = ConfigUtil.getString(ConfigPath.BOT_STATUS);
		this.discordManager.login(botToken, activity);

		// disable if the bot hasn't been invited to the configured guild
		if (this.discordManager.getGuild() == null) {
			String inviteLink = this.discordManager.getBotInvite();
			String message = String.format(
				"Discord bot is not in the configured server,"
				+ " please invite the bot through the following link:\n%s",
				inviteLink
			);

			LoggerUtil.errorAndExit(message);
		}
	}

	@Override
	public final void onDisable() {
		this.discordManager.logout();
		Bukkit.getScheduler().cancelTasks(this);
		if (this.codesManager != null) this.codesManager.getCache().invalidateAll();
		this.database.close();
	}

	public final void reloadPlugin() {
		this.discordManager.logout();
		this.database.close();
		this.reloadConfig();
		this.onEnable();
	}

	@SuppressWarnings("unchecked")
	public static <Type extends Manager> Type getManager(final Class<Type> managerClass) {
		if (managers.containsKey(managerClass)) {
			return (Type) managers.get(managerClass);
		}
		try {
			Constructor<?> constructor = managerClass.getConstructors()[0];
			Type manager = (Type) constructor.newInstance();
			managers.putIfAbsent((Class<Manager>) managerClass, manager);
			return manager;
		} catch (Exception exception) {
			String message = String.format(
				"Failed to instantiate manager \"%s\".",
				managerClass.getSimpleName()
			);

			LoggerUtil.errorAndExit(message);
			return null;
		}
	}

	public static boolean getReady() {
		// if the guild isn't null, we *should* be ready to handle things
		return Arch.getManager(DiscordManager.class).getGuild() != null;
	}

	public static Arch getInstance() {
		return Arch.instance;
	}

	private void registerListeners() {
		this.registerListener(new PlayerPreLoginListener());
		this.registerListener(new PlayerGenericListener());
		this.registerListener(new PlayerChatListener());
	}

	private void registerListener(final Listener listener) {
		this.getServer().getPluginManager().registerEvents(listener, this);
	}
}
