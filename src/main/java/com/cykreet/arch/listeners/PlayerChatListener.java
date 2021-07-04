package com.cykreet.arch.listeners;

import java.util.HashMap;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.WebhookUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {
	private DiscordManager discord = Arch.getInstance().discord;

	@EventHandler
	private void onPlayerChat(AsyncPlayerChatEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getPlayer();
		String playerName = player.getName();
		String playerAvatar = WebhookUtil.getPlayerAvatar(player.getUniqueId());
		placeholders.put("player", player.getName());

		// todo: resolve user mentions
		String message = event.getMessage();
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> this.discord.sendMessage(playerName, playerAvatar, message));
	}
}
