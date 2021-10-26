package com.cykreet.arch.listeners;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CodesManager;
import com.cykreet.arch.managers.DatabaseManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.DatabaseUtil;
import com.cykreet.arch.util.Message;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class DiscordPrivateMessageListener implements MessageCreateListener {
	private final Cache<UUID, String> codesCache = Arch.getManager(CodesManager.class).getCache();
	private final DatabaseManager database = Arch.getManager(DatabaseManager.class);
	private final Cache<String, Boolean> cooldownCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(3))
		.maximumSize(20)
		.build();

	@Override
	public void onMessageCreate(MessageCreateEvent event) {
		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_LINKED)) return;
		if (!event.isPrivateMessage()) return;
		
		PrivateChannel channel = event.getPrivateChannel().get();
		MessageAuthor user = event.getMessageAuthor();
		String userId = user.getIdAsString();
		if (user.isBotUser()) return;
		if (this.cooldownCache.getIfPresent(userId) != null) return;
		this.cooldownCache.put(userId, true);

		String messageContent = event.getReadableMessageContent();
		UUID playerId = this.database.getPlayerId(userId);
		if (playerId == null) {
			// get the user's minecraft player uuid through the cache by
			// it's corresponding code provided by the user
			Entry<UUID, String> cachedPlayerEntry = this.codesCache.asMap().entrySet().stream()
				.filter((code) -> messageContent.equalsIgnoreCase(code.getValue()))
				.findFirst()
				.orElse(null);

			if (cachedPlayerEntry == null) {
				channel.sendMessage(Message.DISCORD_INVALID_CODE.content);
				return;
			}

			UUID playerUUID = cachedPlayerEntry.getKey();
			this.codesCache.invalidate(playerUUID);
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
			this.database.insert(playerUUID, userId);
			String message = ConfigUtil.formatMessage(Message.DISCORD_LINKED_ACCOUNT, player.getName());
			channel.sendMessage(message);
			return;
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
		String playerName = player.getName();
		if (messageContent.equalsIgnoreCase("unlink")) {
			DatabaseUtil.unlinkPlayer(playerId);
			String message = ConfigUtil.formatMessage(Message.DISCORD_UNLINKED_ACCOUNT, playerName);
			channel.sendMessage(message);
			return;
		}

		String message = ConfigUtil.formatMessage(Message.DISCORD_ALREADY_LINKED_ACCOUNT, playerName);
		channel.sendMessage(message);
	}
}
