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
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
	private DiscordManager discord = Arch.getInstance().discord;

	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_DEATH)) return;
		HashMap<String, String> placeholders = new HashMap<String, String>();
		Player player = event.getEntity();
		String playerName = player.getName();
		placeholders.put("player", playerName);
		placeholders.put("message", event.getDeathMessage());

		String deathMessage = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_DEATH, placeholders);
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> this.discord.sendMessage(deathMessage));
	}
}
