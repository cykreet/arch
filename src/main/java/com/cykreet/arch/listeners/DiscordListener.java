package com.cykreet.arch.listeners;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.CodesManager;
import com.cykreet.arch.managers.DatabaseManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.DatabaseUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.enums.ConfigPath;
import com.cykreet.arch.util.enums.Message;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordListener extends ListenerAdapter {
	private static final int COOLDOWN_CACHE_EXPIRE_SECONDS = 3;
	private static final int COOLDOWN_CACHE_MAX_ENTRIES = 20;
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final DatabaseManager database = Arch.getManager(DatabaseManager.class);
	private final Cache<UUID, String> codesCache = Arch.getManager(CodesManager.class).getCache();
	private final Cache<String, Boolean> cooldownCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(DiscordListener.COOLDOWN_CACHE_EXPIRE_SECONDS))
		.maximumSize(DiscordListener.COOLDOWN_CACHE_MAX_ENTRIES)
		.build();

	@Override
	public void onReady(final ReadyEvent event) {
		JDA client = event.getJDA();
		String configChannelId = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		TextChannel configChannel = client.getTextChannelById(configChannelId);

		SelfUser selfUser = client.getSelfUser();
		EnumSet<Permission> permissions = this.discordManager.getUserPermissions(selfUser);
		if (!permissions.contains(Permission.MANAGE_CHANNEL)) return;
		this.discordManager.ensureWebhook(configChannel);

		String configChannelTopic = ConfigUtil.getString(ConfigPath.CHANNEL_TOPIC);
		configChannel.getManager().setTopic(configChannelTopic).queue();
		String guildName = this.discordManager.getGuild().getName();
		String message = String.format(
			"Discord bot \"%s\", is ready and configured for the \"%s\" Discord server.",
			selfUser.getAsTag(),
			guildName
		);

		LoggerUtil.info(message);
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		String discordChannelId = event.getChannel().getId();
		String configChannelId = this.discordManager.getChannel().getId();
		if (event.isWebhookMessage() || !discordChannelId.equals(configChannelId)) return;

		User user = event.getAuthor();
		String userId = user.getId();
		String selfId = this.discordManager.getSelfUser().getId();
		if (userId.equals(selfId)) return;

		HashMap<String, String> placeholders = new HashMap<String, String>();
		String userName = user.getName();
		String userDiscrim = user.getDiscriminator();
		String content = event.getMessage().getContentDisplay();
		placeholders.put("username", userName);
		placeholders.put("discrim", userDiscrim);
		placeholders.put("message", content);

		OfflinePlayer player = Bukkit.getOfflinePlayer(this.database.getPlayerId(userId));
		String message = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_CHAT, placeholders, player);
		Bukkit.broadcastMessage(message);
	}

	@Override
	public void onPrivateMessageReceived(final PrivateMessageReceivedEvent event) {
		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_LINKED)) return;

		PrivateChannel channel = event.getChannel();
		User user = event.getAuthor();
		String userId = user.getId();
		if (user.isBot()) return;
		if (this.cooldownCache.getIfPresent(userId) != null) return;
		this.cooldownCache.put(userId, true);

		String messageContent = event.getMessage().getContentDisplay();
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
			String message = ConfigUtil.formatMessage(Message.DISCORD_LINKED_ACCOUNT, player.getName());
			channel.sendMessage(message).queue();
			return;
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
		String playerName = player.getName();
		if (messageContent.equalsIgnoreCase("unlink")) {
			DatabaseUtil.unlinkPlayer(playerId);
			String message = ConfigUtil.formatMessage(Message.DISCORD_UNLINKED_ACCOUNT, playerName);
			channel.sendMessage(message).queue();
			return;
		}

		String message = ConfigUtil.formatMessage(Message.DISCORD_ALREADY_LINKED_ACCOUNT, playerName);
		channel.sendMessage(message).queue();
	}

	@Override
	public void onGuildBan(final GuildBanEvent event) {
		String userId = event.getUser().getId();
		if (!this.database.contains(userId)) return;
		DatabaseUtil.unlinkPlayer(userId);
	}

	@Override
	public void onGuildMemberRemove(final GuildMemberRemoveEvent event) {
		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_IN_SERVER)) return;
		String discordGuildId = event.getGuild().getId();
		String configGuildId = this.discordManager.getGuild().getId();
		if (!discordGuildId.equals(configGuildId)) return;

		String userId = event.getUser().getId();
		UUID playerUUID = this.database.getPlayerId(userId);
		if (playerUUID == null) return;
		Player player = Bukkit.getPlayer(playerUUID);

		if (player == null || !player.isOnline()) return;
		player.kickPlayer("You are no longer in the required Discord server.");
	}

	@Override
	public void onGuildLeave(final GuildLeaveEvent event) {
		String discordGuildId = event.getGuild().getId();
		String configGuildId = this.discordManager.getGuild().getId();
		if (!discordGuildId.equals(configGuildId)) return;
		LoggerUtil.errorAndExit("Discord bot has forcefully been removed from the configured server.");
	}
}
