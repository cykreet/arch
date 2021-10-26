package com.cykreet.arch.listeners;

import java.util.HashMap;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DatabaseManager;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.DatabaseUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.Message;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.server.member.ServerMemberBanEvent;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.server.member.ServerMemberBanListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;

public class DiscordGuildListener implements MessageCreateListener, ServerMemberBanListener, ServerMemberLeaveListener {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final DatabaseManager database = Arch.getManager(DatabaseManager.class);
	
	@Override
	public void onMessageCreate(MessageCreateEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		String discordServerId = event.getServer().get().getIdAsString();
		String configServerId = this.discordManager.getGuild().getIdAsString();
		if (!event.isServerMessage() || !discordServerId.equals(configServerId)) return;

		MessageAuthor user = event.getMessageAuthor();
		String userId = user.getIdAsString(); 
		String selfId = this.discordManager.getSelfUser().getIdAsString();
		if (user.isWebhook() || userId.equals(selfId)) return;
		
		HashMap<String, String> placeholders = new HashMap<String, String>();
		String userName = user.getName();
		String userDiscrim = user.getDiscriminator().get();
		String content = event.getReadableMessageContent();
		placeholders.put("username", userName);
		placeholders.put("discrim", userDiscrim);
		placeholders.put("message", content);
		
		OfflinePlayer player = Bukkit.getOfflinePlayer(this.database.getPlayerId(userId));
		String message = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_CHAT, placeholders, player);
		Bukkit.broadcastMessage(message);
	}

	@Override
	public void onServerMemberBan(ServerMemberBanEvent event) {
		String userId = event.getUser().getIdAsString();
		if (!this.database.contains(userId)) return;
		DatabaseUtil.unlinkPlayer(userId);
	}

	@Override
	public void onServerMemberLeave(ServerMemberLeaveEvent event) {
		String guildId = event.getServer().getIdAsString();
		String configServerId = this.discordManager.getGuild().getIdAsString();
		if (guildId.equals(configServerId)) return;

		String userId = event.getUser().getIdAsString();
		String botId = this.discordManager.getSelfUser().getIdAsString();
		if (userId.equals(botId)) {
			LoggerUtil.errorAndExit(Message.INTERNAL_BOT_REMOVED.content);
			return;
		}

		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_IN_SERVER)) return;
		UUID playerUUID = this.database.getPlayerId(userId);
		if (playerUUID == null) return;
		Player player = Bukkit.getPlayer(playerUUID);

		if (player == null || !player.isOnline()) return;
		player.kickPlayer("You are no longer in the required Discord server.");
	}
}
