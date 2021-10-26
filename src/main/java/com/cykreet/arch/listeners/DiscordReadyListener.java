package com.cykreet.arch.listeners;

import com.cykreet.arch.Arch;
import com.cykreet.arch.managers.DiscordManager;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.Message;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.ServerBecomesAvailableEvent;
import org.javacord.api.listener.server.ServerBecomesAvailableListener;

public class DiscordReadyListener implements ServerBecomesAvailableListener {
	private final DiscordManager discordManager = Arch.getManager(DiscordManager.class);

	@Override
	public void onServerBecomesAvailable(ServerBecomesAvailableEvent event) {
		DiscordApi client = event.getApi();
		User selfUser = client.getYourself();
		String configChannelId = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		ServerTextChannel configChannel = client.getServerTextChannelById(configChannelId).get();
		this.discordManager.ensureWebhook(configChannel);

		String configChannelTopic = ConfigUtil.getString(ConfigPath.CHANNEL_TOPIC);
		configChannel.updateTopic(configChannelTopic);
		String message = ConfigUtil.formatMessage(Message.INTERNAL_BOT_READY, selfUser.getDiscriminatedName());
		LoggerUtil.info(message);
	}
}
