package com.cykreet.arch.listeners;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.enums.ConfigPath;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerChatListener implements Listener {
	private DiscordManager discordManager = Arch.getManager(DiscordManager.class);

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerChat(final AsyncPlayerChatEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		if (event.isCancelled()) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getPlayer();
		String playerName = player.getName();
		placeholders.put("player", player.getName());

		// todo: resolve user mentions
		String message = event.getMessage();
		URL playerAvatar = PlayerChatListener.getPlayerAvatar(player.getUniqueId());
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () ->
			this.discordManager.sendWebhookMessage(playerName, playerAvatar, message));
	}

	@Nullable
	public static URL getPlayerAvatar(@NotNull final UUID playerUUID) {
		String avatar = PlayerChatListener.formatPlayerURL(playerUUID);
		try {
			return new URL(avatar);
		} catch (MalformedURLException err) {
			LoggerUtil.error("Malformed player avatar:", err);
			return null;
		}
	}

	public static String formatPlayerURL(@NotNull final UUID playerUUID) {
		return String.format("https://crafatar.com/avatars/%s.png?size=120&overlay", playerUUID.toString());
	}
}
