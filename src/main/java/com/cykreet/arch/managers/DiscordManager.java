package com.cykreet.arch.managers;

import java.net.URL;
import java.util.List;

import com.cykreet.arch.Arch;
import com.cykreet.arch.listeners.DiscordGuildListener;
import com.cykreet.arch.listeners.DiscordPrivateMessageListener;
import com.cykreet.arch.listeners.DiscordReadyListener;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.Message;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentions;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.jetbrains.annotations.NotNull;

public class DiscordManager extends Manager {
	private Webhook webhook;
	private DiscordApi client;
	private final Intent[] intents = {
		Intent.GUILD_MEMBERS,
		Intent.DIRECT_MESSAGES,
		Intent.GUILD_WEBHOOKS,
		Intent.GUILD_MESSAGES
	};

	public void login(@NotNull String token, @NotNull String activity) {
		DiscordGuildListener discordGuildListener = new DiscordGuildListener();
		this.client = new DiscordApiBuilder()
			.addServerBecomesAvailableListener(new DiscordReadyListener())
			.addMessageCreateListener(new DiscordPrivateMessageListener())
			.addMessageCreateListener(discordGuildListener)
			.addServerMemberBanListener(discordGuildListener)
			.addServerMemberLeaveListener(discordGuildListener)
			.setIntents(intents)
			.setToken(token)
			.login()
			.join();

		this.client.setMessageCacheSize(3, 1);
		this.client.updateActivity(activity);
	}

	public void logout() {
		if (this.client == null) return;
		this.client.disconnect();
		this.client = null;
	}

	public void ensureWebhook(@NotNull ServerTextChannel channel) {
		if (this.webhook != null) return;
		User selfUser = this.client.getYourself();
		String name = Arch.getInstance().getName();

		channel.getWebhooks().thenAccept((List<Webhook> webhooks) -> {
			Webhook configWebhook = webhooks.stream().filter((Webhook webhook) -> (
				webhook.getCreator().orElse(null).getIdAsString().equals(selfUser.getIdAsString())
			))
			.findFirst()
			.orElse(null);

			if (configWebhook != null) this.webhook = configWebhook;
			else this.webhook = channel.createWebhookBuilder().setName(name).create().join();
		});
	}

	public void sendWebhookMessage(@NotNull String name, @NotNull URL avatar, @NotNull String message) {
		if (this.webhook == null) {
			LoggerUtil.error(Message.INTERNAL_WEBHOOK_NOT_CREATED.content);
			return;
		}
		
		IncomingWebhook incomingWebhook = this.webhook.asIncomingWebhook().get();
		AllowedMentions allowedMentions = new AllowedMentionsBuilder().setMentionUsers(true).build();
		new WebhookMessageBuilder()
			.setDisplayName(name)
			.setDisplayAvatar(avatar)
			.setAllowedMentions(allowedMentions)
			.setContent(message)
			.send(incomingWebhook);
	}

	public void sendMessage(@NotNull String message) {
		ServerTextChannel channel =  this.getChannel();
		channel.sendMessage(message);
	}

	public ServerTextChannel getChannel() {
		String configChannelId = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		return this.client.getServerTextChannelById(configChannelId).get();
	}

	public Server getGuild() {
		ServerChannel channel = this.getChannel();
		if (channel == null) return null;
		return channel.getServer();
	}

	public Role getRequiredRole() {
		if (ConfigUtil.contains(ConfigPath.AUTH_NOT_LINKED) != true) return null;
		String configRoleId = ConfigUtil.getString(ConfigPath.AUTH_REQUIRED_ROLE);
		if (configRoleId == null) return null;
		Role role = this.client.getRoleById(configRoleId).get();
		return role;
	}

	public User getSelfUser() {
		return this.client.getYourself();
	}

	public String getBotInvite() {
		Permissions botPermissions = new PermissionsBuilder().setAllowed(
			PermissionType.MANAGE_ROLES, 
			PermissionType.SEND_MESSAGES, 
			PermissionType.EMBED_LINKS, 
			PermissionType.READ_MESSAGES, 
			PermissionType.MANAGE_WEBHOOKS
		)
		.build();

		return this.client.createBotInvite(botPermissions);
	}
}
