package com.cykreet.arch.listeners;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CacheManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.managers.PersistManager;
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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;

public class PlayerPreLoginListener implements Listener {
	private final Cache<UUID, String> codesCache = Arch.getManager(CacheManager.class).getCache();
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final PersistManager database = Arch.getManager(PersistManager.class);
	private static final char[] characters = "0213546879".toCharArray();

	@EventHandler(priority = EventPriority.LOW)
	private void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUniqueId());
		if (Bukkit.getIPBans().contains(event.getAddress().getHostAddress())) return;
		if (Bukkit.getBannedPlayers().contains(player)) return;

		UUID playerUUID = event.getUniqueId();
		Guild guild = this.discordManager.getGuild();
		if (!this.database.contains(playerUUID)) {
			String code = this.codesCache.getIfPresent(playerUUID);
			if (code == null) {
				String codePrefix = player.getName().charAt(0) + "-";
				code = codePrefix.toUpperCase().concat(generateRandomCode());
				this.codesCache.put(playerUUID, code);
			}

			HashMap<String, String> placeholders = new HashMap<String, String>();
			int codeExpiry = ConfigUtil.getInt(ConfigPath.AUTH_CODE_EXPIRE);
			SelfUser selfUser = this.discordManager.getSelfUser();
			placeholders.put("code", code);
			placeholders.put("bot", selfUser.getAsTag());
			placeholders.put("code.ttl", Integer.toString(codeExpiry));
			placeholders.put("server", guild.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_NOT_LINKED, placeholders);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}

		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_IN_SERVER)) return;
		String discordId = this.database.getMemberId(playerUUID);
		Member guildMember = guild.getMemberById(discordId);
		if (guildMember == null) {
			HashMap<String, String> placeholders = new HashMap<String, String>();
			placeholders.put("server", guild.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_NOT_IN_SERVER, placeholders);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}

		if (!ConfigUtil.contains(ConfigPath.AUTH_REQUIRED_ROLE)) return;
		Role requiredRole = this.discordManager.getRequiredRole();
		if (!guildMember.getRoles().contains(requiredRole)) {
			HashMap<String, String> placeholders = new HashMap<String, String>();
			placeholders.put("server", guild.getName());
			placeholders.put("role", requiredRole.getName());

			String kickMessage = ConfigUtil.getString(ConfigPath.AUTH_ABSENT_ROLE, placeholders);
			event.disallow(Result.KICK_OTHER, kickMessage);
			return;
		}
	}

	private static String generateRandomCode() {
		char buffer[] = new char[4];
		Random random = new Random();
		for (int i = 0; i < buffer.length; i++) {
			int index = random.nextInt(characters.length);
			buffer[i] = characters[index];
		}

		return new String(buffer);
	}
}
