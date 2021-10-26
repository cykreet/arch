package com.cykreet.arch.util;

import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DatabaseManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class DatabaseUtil {
	private static final DatabaseManager database = Arch.getManager(DatabaseManager.class);

	public static void unlinkPlayer(@NotNull String discordId) {
		database.removeByMemberId(discordId);
		UUID playerUUID = database.getPlayerId(discordId);
		DatabaseUtil.kickUnlinkedPlayer(playerUUID);
	}

	public static void unlinkPlayer(@NotNull UUID playerUUID) {
		database.removeByPlayerId(playerUUID);
		DatabaseUtil.kickUnlinkedPlayer(playerUUID);
	}

	private static void kickUnlinkedPlayer(@NotNull UUID playerUUID) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
		if (!offlinePlayer.isOnline()) return;
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> {
			offlinePlayer.getPlayer().kickPlayer("Unlinked Discord account.");
		});
	}
}
