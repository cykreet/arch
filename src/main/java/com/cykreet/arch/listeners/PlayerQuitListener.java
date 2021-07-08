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
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);

	@EventHandler
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
}
