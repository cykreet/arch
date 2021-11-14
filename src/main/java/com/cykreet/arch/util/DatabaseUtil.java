package com.cykreet.arch.util;

import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DatabaseManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class DatabaseUtil {
	private static final DatabaseManager DATABASE_MANAGER = Arch.getManager(DatabaseManager.class);

	public static void unlinkPlayer(@NotNull final String discordId) {
		DATABASE_MANAGER.removeByMemberId(discordId);
		UUID playerUUID = DATABASE_MANAGER.getPlayerId(discordId);
		DatabaseUtil.kickUnlinkedPlayer(playerUUID);
	}

	public static void unlinkPlayer(@NotNull final UUID playerUUID) {
		DATABASE_MANAGER.removeByPlayerId(playerUUID);
		DatabaseUtil.kickUnlinkedPlayer(playerUUID);
	}

	private static void kickUnlinkedPlayer(@NotNull final UUID playerUUID) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
		if (!offlinePlayer.isOnline()) return;
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> {
			offlinePlayer.getPlayer().kickPlayer("Unlinked Discord account.");
		});
	}
}
