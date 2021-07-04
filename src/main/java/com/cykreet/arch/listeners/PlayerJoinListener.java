package com.cykreet.arch.listeners;

import java.util.HashMap;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
	private final DiscordManager discord = Arch.getInstance().discord;

	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_JOIN)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getPlayer();
		String playerName = player.getName();
		placeholders.put("player", playerName);

		String joinMessage = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_JOIN, placeholders);
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> this.discord.sendMessage(joinMessage));
	}
}
