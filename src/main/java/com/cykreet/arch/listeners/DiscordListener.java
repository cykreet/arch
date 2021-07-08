package com.cykreet.arch.listeners;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CacheManager;
import com.cykreet.arch.managers.ConfigManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.managers.PersistManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordListener extends ListenerAdapter {
	private final Cache<UUID, String> codesCache = Arch.getManager(CacheManager.class).getCache();
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final ConfigManager configManager = Arch.getManager(ConfigManager.class);
	private final PersistManager database = Arch.getManager(PersistManager.class);
	private final Cache<String, Boolean> cooldownCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(3))
		.maximumSize(20)
		.build();

	@Override
	public void onReady(ReadyEvent event) {
		SelfUser bot = event.getJDA().getSelfUser();
		String configChannel = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		TextChannel channel = event.getJDA().getTextChannelById(configChannel);
		this.discordManager.ensureWebhook(channel);

		String configChannelTopic = ConfigUtil.getString(ConfigPath.CHANNEL_TOPIC);
		channel.getManager().setTopic(configChannelTopic).queue();
		String message = String.format("Discord bot \"%s\", is ready.", bot.getAsTag());
		LoggerUtil.info(message);
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (!this.configManager.getAuthenticationEnabled()) return;

		User user = event.getAuthor();
		String userId = user.getId();
		if (this.cooldownCache.getIfPresent(userId) != null) return;
		this.cooldownCache.put(userId, true);
		if (user.isBot()) return;

		PrivateChannel channel = event.getChannel();
		String messageContent = event.getMessage().getContentStripped();
		UUID playerId = this.database.getPlayerId(userId);
		if (playerId == null) {
			// get the user's minecraft player uuid through the cache by
			// it's corresponding code provided by the user
			Entry<UUID, String> cachedPlayerEntry = this.codesCache.asMap().entrySet().stream()
				.filter((code) -> messageContent.equals(code.getValue()))
				.findFirst()
				.orElse(null);

			if (cachedPlayerEntry == null) {
				String message = "Invalid code provided, please make sure the code you're trying to use hasn't expired and try again.";
				channel.sendMessage(message).queue();
				return;
			}

			UUID playerUUID = cachedPlayerEntry.getKey();
			this.codesCache.invalidate(playerUUID);
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
			this.database.insert(playerUUID, userId);
			String message = String.format("Your Discord account has been linked to `%s`.", player.getName());
			channel.sendMessage(message).queue();
			return;
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
		String playerName = player.getName();
		if (messageContent.equalsIgnoreCase("unlink")) {
			this.database.unlinkPlayer(playerId);
			String message = String.format("Your Discord account has been unlinked from `%s`.", playerName);
			channel.sendMessage(message).queue();
			return;
		}

		String message = String.format(
			"Your Discord account has already been linked to `%s`. If you'd like to unlink your account, reply with `unlink`.", 
			playerName
		);
		channel.sendMessage(message).queue();
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		User user = event.getAuthor();
		SelfUser selfUser = this.discordManager.getSelfUser();
		if (event.isWebhookMessage() || user.getId() == selfUser.getId()) return;

		HashMap<String, String> placeholders = new HashMap<String, String>();
		String userName = user.getName();
		String userDiscrim = user.getDiscriminator();
		String content = event.getMessage().getContentStripped();
		placeholders.put("username", userName);
		placeholders.put("discrim", userDiscrim);
		placeholders.put("message", content);

		String message = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_CHAT, placeholders);
		Bukkit.broadcastMessage(message);
	}
}
