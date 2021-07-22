package com.cykreet.arch.listeners;

import java.util.HashMap;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerGenericListener implements Listener {
	private DiscordManager discordManager = Arch.getManager(DiscordManager.class);

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerQuit(PlayerQuitEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_LEAVE)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getPlayer();
		String playerName = player.getName();
		placeholders.put("player", playerName);

		String quitMessage = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_LEAVE, placeholders);
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> 
			this.discordManager.sendMessage(quitMessage));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerJoin(PlayerJoinEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_JOIN)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getPlayer();
		String playerName = player.getName();
		placeholders.put("player", playerName);

		String joinMessage = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_JOIN, placeholders);
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> 
			this.discordManager.sendMessage(joinMessage));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerDeath(PlayerDeathEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_DEATH)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getEntity();
		String playerName = player.getName();
		placeholders.put("player", playerName);
		placeholders.put("message", event.getDeathMessage());

		String deathMessage = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_DEATH, placeholders);
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> 
			this.discordManager.sendMessage(deathMessage));
	}
}
