package com.cykreet.arch.listeners;

import java.util.HashMap;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CodesManager;
import com.cykreet.arch.managers.DatabaseManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.github.benmanes.caffeine.cache.Cache;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class PlayerPreLoginListener implements Listener {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final CodesManager codesManager = Arch.getManager(CodesManager.class);
	private final DatabaseManager database = Arch.getManager(DatabaseManager.class);

	@EventHandler(priority = EventPriority.LOW)
	private void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_LINKED)) return;
		UUID playerUUID = event.getUniqueId();
		if (Bukkit.getIPBans().contains(event.getAddress().getHostAddress())) return;
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
		if (Bukkit.getBannedPlayers().contains(player)) return;
		if (Arch.getReady() != true) {
			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_NOT_READY);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}

		Server guild = this.discordManager.getGuild();
		if (!this.database.contains(playerUUID)) {
			Cache<UUID, String> codesCache = this.codesManager.getCache();
			String code = codesCache.getIfPresent(playerUUID);
			if (code == null) {
				String codePrefix = player.getName().charAt(0) + "-";
				code = codePrefix.toUpperCase().concat(this.codesManager.generateRandomCode());
				codesCache.put(playerUUID, code);
			}

			HashMap<String, String> placeholders = new HashMap<String, String>();
			int codeExpiry = ConfigUtil.getInt(ConfigPath.AUTH_CODE_EXPIRE);
			User selfUser = this.discordManager.getSelfUser();
			placeholders.put("code", code);
			placeholders.put("bot", selfUser.getDiscriminatedName());
			placeholders.put("code.ttl", Integer.toString(codeExpiry));
			placeholders.put("server", guild.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_NOT_LINKED, placeholders, player);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}

		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_IN_SERVER)) return;
		String discordId = this.database.getMemberId(playerUUID);
		User discordUser = guild.getMemberById(discordId).get();
		if (discordUser == null) {
			HashMap<String, String> placeholders = new HashMap<String, String>();
			placeholders.put("server", guild.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_NOT_IN_SERVER, placeholders, player);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}

		if (!ConfigUtil.contains(ConfigPath.AUTH_REQUIRED_ROLE)) return;
		Role requiredRole = this.discordManager.getRequiredRole();
		if (!discordUser.getRoles(guild).contains(requiredRole)) {
			HashMap<String, String> placeholders = new HashMap<String, String>();
			placeholders.put("server", guild.getName());
			placeholders.put("role", requiredRole.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_ABSENT_ROLE, placeholders, player);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}
	}
}
