package com.cykreet.arch.listeners;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CacheManager;
import com.cykreet.arch.managers.PersistManager;
import com.cykreet.arch.util.Message;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordPrivateMessageListener extends ListenerAdapter {
	private final Cache<UUID, String> codesCache = Arch.getManager(CacheManager.class).getCache();
	private final PersistManager database = Arch.getManager(PersistManager.class);
	private final Cache<String, Boolean> cooldownCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(3))
		.maximumSize(20)
		.build();

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		User user = event.getAuthor();
		String userId = user.getId();
		if (user.isBot()) return;
		if (this.cooldownCache.getIfPresent(userId) != null) return;
		this.cooldownCache.put(userId, true);

		PrivateChannel channel = event.getChannel();
		String messageContent = event.getMessage().getContentStripped();
		UUID playerId = this.database.getPlayerId(userId);
		if (playerId == null) {
			// get the user's minecraft player uuid through the cache by
			// it's corresponding code provided by the user
			Entry<UUID, String> cachedPlayerEntry = this.codesCache.asMap().entrySet().stream()
				.filter((code) -> messageContent.equalsIgnoreCase(code.getValue()))
				.findFirst()
				.orElse(null);

			if (cachedPlayerEntry == null) {
				channel.sendMessage(Message.DISCORD_INVALID_CODE.content).queue();
				return;
			}

			UUID playerUUID = cachedPlayerEntry.getKey();
			this.codesCache.invalidate(playerUUID);
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
			this.database.insert(playerUUID, userId);
			String message = String.format(Message.DISCORD_LINKED_ACCOUNT.content, player.getName());
			channel.sendMessage(message).queue();
			return;
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
		String playerName = player.getName();
		if (messageContent.equalsIgnoreCase("unlink")) {
			this.database.unlinkPlayer(playerId);
			String message = String.format(Message.DISCORD_UNLINKED_ACCOUNT.content, playerName);
			channel.sendMessage(message).queue();
			return;
		}

		String message = String.format(Message.DISCORD_ALREADY_LINKED_ACCOUNT.content, playerName);
		channel.sendMessage(message).queue();
	}
}
