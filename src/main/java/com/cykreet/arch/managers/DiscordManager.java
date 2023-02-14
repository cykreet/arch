package com.cykreet.arch.managers;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import com.cykreet.arch.Arch;
import com.cykreet.arch.listeners.DiscordListener;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.enums.ConfigPath;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordManager extends Manager {
	private Webhook destinationWebhook;
	private JDA client;
	public static final Permission[] PERMISSIONS = {
		Permission.VIEW_CHANNEL,
		Permission.MESSAGE_SEND,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MANAGE_WEBHOOKS,
		Permission.MANAGE_CHANNEL,
		Permission.MANAGE_ROLES
	};
	private final GatewayIntent[] intents = {
		GatewayIntent.GUILD_MEMBERS,
		GatewayIntent.DIRECT_MESSAGES,
		GatewayIntent.GUILD_WEBHOOKS,
		GatewayIntent.GUILD_MESSAGES,
		GatewayIntent.MESSAGE_CONTENT
	};
	private final CacheFlag[] disabledCaches = {
		CacheFlag.EMOJI,
		CacheFlag.ACTIVITY,
		CacheFlag.VOICE_STATE,
		CacheFlag.ONLINE_STATUS,
		CacheFlag.CLIENT_STATUS,
		CacheFlag.MEMBER_OVERRIDES,
		CacheFlag.STICKER,
		CacheFlag.SCHEDULED_EVENTS
	};

	public void login(@NotNull final String token) {
		try {
			JDABuilder builder = JDABuilder.create(token, Arrays.asList(intents));
			builder.setActivity(Activity.playing(ConfigUtil.getString(ConfigPath.BOT_STATUS)));
			builder.disableCache(Arrays.asList(disabledCaches));
			builder.addEventListeners(new DiscordListener());
			builder.setToken(token);

			this.client = builder.build();
			this.client.awaitReady();
			// todo: gateway intent was not granted, could give a warning
		} catch (IllegalStateException exception) {
			LoggerUtil.errorAndExit("Failed to establish connection with Discord:", exception);
		} catch (Exception exception) {
			LoggerUtil.errorAndExit("An unknown error occurred whilst starting the bot:", exception);
		}
	}

	public void logout() {
		if (this.client == null) return;
		this.client.shutdown();
		this.client = null;
	}

	public void ensureWebhook(@NotNull final TextChannel channel) {
		if (this.destinationWebhook != null) return;
		SelfUser selfUser = this.client.getSelfUser();
		String name = Arch.getInstance().getName();
		channel.retrieveWebhooks().queue((webhooks) -> {
			Webhook configWebhook = webhooks.stream()
			.filter((webhook) -> webhook.getOwner().getId().equals(selfUser.getId()))
			.findFirst()
			.orElse(null);

			if (configWebhook != null) this.destinationWebhook = configWebhook;
			else channel.createWebhook(name).queue((webhook) -> {
				this.destinationWebhook = webhook;
			});
		});
	}

	public void sendWebhookMessage(
		@NotNull final String name,
		@NotNull final String avatar,
		@NotNull final String message
	) {
		if (this.destinationWebhook == null) {
			LoggerUtil.error("Manageable webhook has not been created.");
			return;
		}

		try {
			URL webhookUrl =  new URL(this.destinationWebhook.getUrl());
			HttpURLConnection connection = (HttpURLConnection) webhookUrl.openConnection();
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.connect();

			JsonArray allowedMentionsUsers = new JsonArray();
			allowedMentionsUsers.add("users");
			JsonObject allowedMentions = new JsonObject();
			allowedMentions.add("parse", allowedMentionsUsers);

			JsonObject body = new JsonObject();
			body.addProperty("username", name);
			body.addProperty("avatar_url", avatar);
			body.addProperty("content", message);
			body.add("allowed_mentions", allowedMentions);

			OutputStream os = connection.getOutputStream();
			String bodyString = new Gson().toJson(body);
			byte[] input = bodyString.getBytes("utf-8");
			os.write(input, 0, input.length);
			os.flush();
			os.close();

			if (connection.getResponseCode() > HttpURLConnection.HTTP_BAD_REQUEST) {
				throw new Exception(connection.getResponseMessage());
			}
		} catch (Exception exception) {
			LoggerUtil.error("Failed to execute webhook:", exception);
		}
	}

	public void sendMessage(@NotNull final String message) {
	 	TextChannel channel = this.getChannel();
		channel.sendMessage(message).queue();
	}

	public TextChannel getChannel() {
		String configChannelId = ConfigUtil.getString(ConfigPath.CHANNEL_ID);
		return this.client.getTextChannelById(configChannelId);
	}

	public Guild getGuild() {
		TextChannel channel = this.getChannel();
		if (channel == null) return null;
		return channel.getGuild();
	}

	public Role getRequiredRole() {
		if (!ConfigUtil.contains(ConfigPath.AUTH_NOT_LINKED)) return null;
		String configRoleId = ConfigUtil.getString(ConfigPath.AUTH_REQUIRED_ROLE);
		if (configRoleId == null) return null;

		Role role = this.getGuild().getRoleById(configRoleId);
		return role;
	}

	public EnumSet<Permission> getUserPermissions(final User user) {
		Guild guild = this.getGuild();
		TextChannel channel = this.getChannel();
		return guild.getMember(user).getPermissions(channel);
	}

	public SelfUser getSelfUser() {
		return this.client.getSelfUser();
	}

	public String getBotInvite() {
		return this.client.getInviteUrl(DiscordManager.PERMISSIONS);
	}
}
