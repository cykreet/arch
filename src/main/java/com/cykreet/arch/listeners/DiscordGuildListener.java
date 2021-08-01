package com.cykreet.arch.listeners;

import java.util.HashMap;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.managers.PersistManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.Message;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordGuildListener extends ListenerAdapter {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);
	private final PersistManager database = Arch.getManager(PersistManager.class);
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!ConfigUtil.contains(ConfigPath.MESSAGE_FORMAT_CHAT)) return;
		User user = event.getAuthor();
		String userId = user.getId(); 
		String selfId = this.discordManager.getSelfUser().getId();
		if (event.isWebhookMessage() || userId.equals(selfId)) return;
		
		HashMap<String, String> placeholders = new HashMap<String, String>();
		String userName = user.getName();
		String userDiscrim = user.getDiscriminator();
		String content = event.getMessage().getContentStripped();
		placeholders.put("username", userName);
		placeholders.put("discrim", userDiscrim);
		placeholders.put("message", content);
		
		// todo: database results should probably be cached somehow
		OfflinePlayer player = Bukkit.getOfflinePlayer(this.database.getPlayerId(userId));
		String message = ConfigUtil.getString(ConfigPath.MESSAGE_FORMAT_CHAT, placeholders, player);
		Bukkit.broadcastMessage(message);
	}

	@Override
	public void onGuildBan(GuildBanEvent event) {
		// todo: cba to find the kick event
		String userId = event.getUser().getId();
		if (!this.database.contains(userId)) return;
		this.database.unlinkPlayer(userId);
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		String guildId = event.getGuild().getId();
		if (guildId != this.discordManager.getGuild().getId()) return;
		LoggerUtil.errorAndExit(Message.INTERNAL_BOT_REMOVED.content);
	}
}
