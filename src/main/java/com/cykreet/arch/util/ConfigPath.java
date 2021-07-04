package com.cykreet.arch.util;

public enum ConfigPath {
	BOT_TOKEN("discord_bot.token"),
	BOT_STATUS("discord_bot.status?"),
	CHANNEL_ID("channel.id"),
	CHANNEL_TOPIC("channel.topic?"),
	DEFAULT_PLAYER("player?"),
	AUTH_REQUIRED_ROLE("authentication.require_role>"),
	AUTH_CODE_EXPIRE("authentication.code_expires"),
	AUTH_MESSAGE_NOT_LINKED("authentication.message.not_linked?"),
	AUTH_MESSAGE_NOT_IN_SERVER("authentication.message.not_in_server?"),
	AUTH_MESSAGE_ABSENT_ROLE("authentication.message.absent_role?"),
	MESSAGE_FORMAT_JOIN("message_format.join?"),
	MESSAGE_FORMAT_LEAVE("message_format.leave?"),
	MESSAGE_FORMAT_CHAT("message_format.chat?"),
	MESSAGE_FORMAT_DEATH("message_format.death?");

	public final String label;

	private ConfigPath(String label) {
		this.label = label;
	}
}
