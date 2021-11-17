package com.cykreet.arch.managers;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import javax.security.auth.login.LoginException;

import com.cykreet.arch.Arch;
import com.cykreet.arch.listeners.DiscordListener;
import com.cykreet.arch.util.ConfigUtil;
import com.cykreet.arch.util.LoggerUtil;
import com.cykreet.arch.util.enums.ConfigPath;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordManager extends Manager {
	public static final Permission[] PERMISSIONS = {
		Permission.MESSAGE_READ,
		Permission.MESSAGE_WRITE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MANAGE_WEBHOOKS,
		Permission.MANAGE_ROLES
	};
	private Webhook destinationWebhook;
	private JDA client;
	private final GatewayIntent[] intents = {
		GatewayIntent.GUILD_MEMBERS,
		GatewayIntent.DIRECT_MESSAGES,
		GatewayIntent.GUILD_WEBHOOKS,
		GatewayIntent.GUILD_MESSAGES,
	};
	private final CacheFlag[] disabledCaches = {
		CacheFlag.EMOTE,
		CacheFlag.VOICE_STATE,
		CacheFlag.MEMBER_OVERRIDES
	};

	public void login(@NotNull final String token, @NotNull final String activity) {
		JDABuilder builder = JDABuilder.create(token, Arrays.asList(intents));
		builder.disableCache(Arrays.asList(disabledCaches));
		builder.setActivity(Activity.playing(activity));
		builder.addEventListeners(new DiscordListener());
		builder.setToken(token);

		try {
			this.client = builder.build();
			this.client.awaitReady();
		} catch (LoginException exception) {
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
			webhooks.removeIf((webhook) -> !webhook.getOwner().getId().equals(selfUser.getId()));
			Webhook configWebhook = webhooks.get(0);
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

			JsonArray allowedMentions = new JsonArray();
			allowedMentions.add("users");
			JsonObject body = new JsonObject();
			body.addProperty("username", name);
			body.addProperty("avatar_url", avatar);
			body.addProperty("content", message);
			body.add("allowed_mentions", allowedMentions);


			OutputStream os = connection.getOutputStream();
			byte[] input = body.getAsString().getBytes("utf-8");
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

	public SelfUser getSelfUser() {
		return this.client.getSelfUser();
	}

	public String getBotInvite() {
		return this.client.getInviteUrl(DiscordManager.PERMISSIONS);
	}
}
