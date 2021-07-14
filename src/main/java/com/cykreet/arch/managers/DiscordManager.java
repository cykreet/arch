package com.cykreet.arch.managers;

import java.net.URL;
import java.util.EnumSet;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.cykreet.arch.listeners.DiscordListener;
import com.cykreet.arch.util.ConfigPath;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.WebhookUtil;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordManager extends Manager {
	private Webhook webhook;
	private JDA client;
	private final EnumSet<CacheFlag> disabledCaches = EnumSet.of(
		CacheFlag.MEMBER_OVERRIDES,
		CacheFlag.ACTIVITY,
		CacheFlag.EMOTE,
		CacheFlag.CLIENT_STATUS,
		CacheFlag.VOICE_STATE,
		CacheFlag.ONLINE_STATUS
	);
	private final EnumSet<GatewayIntent> intents = EnumSet.of(
		GatewayIntent.GUILD_MEMBERS,
		GatewayIntent.GUILD_MESSAGES,
		GatewayIntent.DIRECT_MESSAGES
	);

	public void login(@NotNull String token, @NotNull String activity) {
		try {
			this.client = JDABuilder.create(this.intents)
				.disableCache(this.disabledCaches)
				.addEventListeners(new DiscordListener())
				// could get kinda problematic with larger guild sizes
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.setActivity(Activity.playing(activity))
				.setContextEnabled(false)
				.setToken(token)
				.build();

			this.client.awaitReady();
		} catch (LoginException exception) {
			LoggerUtil.errorAndExit("Failed to login as Discord bot:\n" + exception.getStackTrace());
		} catch (Exception exception) {
			LoggerUtil.errorAndExit("An unknown error occurred whilst trying to start the Discord bot: \n" + exception.getStackTrace());
		}
	}

	public void logout() {
		if (this.client == null) return;
		this.client.shutdown();
	}

	public void ensureWebhook(@NotNull TextChannel channel) {
		if (this.webhook != null) return;
		String name = "Arch - " + this.client.getSelfUser().getId();
		RestAction<List<Webhook>> getWebhooks = channel.retrieveWebhooks();
		Webhook targetWebhook = getWebhooks.submit().join()
			.stream()
			.filter((webhook) -> webhook.getName().equalsIgnoreCase(name))
			.findFirst()
			.orElse(null);

		if (targetWebhook != null) this.webhook = targetWebhook;
		else this.webhook = channel.createWebhook(name).submit().join();
	}

	public void sendMessage(@NotNull String name, @NotNull String avatar, @NotNull String message) {
		if (this.webhook == null) throw new Error("Managable webhook has not been created.");
		try {
			URL webhookUrl = new URL(this.webhook.getUrl());
			String allowedMentions = "{\"parse\": [\"users\"]}";
			String body = String.format(
				"{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\", \"parse\": %s}",
				name, avatar, message, allowedMentions
			);

			WebhookUtil.post(webhookUrl, body);
		} catch (Exception exception) {
			LoggerUtil.error("Failed to execute webhook:\n" + exception.getStackTrace());
		}
	}

	public void sendMessage(@NotNull String message) {
		TextChannel channel = (TextChannel) this.getChannel();
		channel.sendMessage(message).queue();
	}

	public GuildChannel getChannel() {
		String configChannelId = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		return this.client.getGuildChannelById(configChannelId);
	}

	public Guild getGuild() {
		GuildChannel channel = this.getChannel();
		if (channel == null) return null;
		return channel.getGuild();
	}

	public Role getRequiredRole() {
		String configRoleId = ConfigUtil.getString(ConfigPath.AUTH_REQUIRED_ROLE);
		Role role = this.client.getRoleById(configRoleId);
		return role;
	}

	public SelfUser getSelfUser() {
		return this.client.getSelfUser();
	}
}
