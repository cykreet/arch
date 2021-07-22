package com.cykreet.arch.listeners;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.Message;

import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordReadyListener extends ListenerAdapter {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);

	@Override
	public void onReady(ReadyEvent event) {
		SelfUser bot = event.getJDA().getSelfUser();
		String configChannel = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		TextChannel channel = event.getJDA().getTextChannelById(configChannel);
		this.discordManager.ensureWebhook(channel);

		String configChannelTopic = ConfigUtil.getString(ConfigPath.CHANNEL_TOPIC);
		channel.getManager().setTopic(configChannelTopic).queue();
		String message = String.format(Message.DISCORD_BOT_READY.content, bot.getAsTag());
		LoggerUtil.info(message);
	}
}
