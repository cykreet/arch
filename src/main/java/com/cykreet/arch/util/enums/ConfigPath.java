package com.cykreet.arch.util.enums;

public enum ConfigPath {
	BOT_TOKEN("discord_bot.token"),
	BOT_STATUS("discord_bot.status?"),
	CHANNEL_ID("channel.id"),
	CHANNEL_TOPIC("channel.topic?"),
	DEFAULT_PLAYER("player?"),
	AUTH_NOT_READY("authentication.not_ready"),
	AUTH_REQUIRED_ROLE("authentication.require_role?"),
	AUTH_CODE_EXPIRE("authentication.code_expires"),
	AUTH_NOT_LINKED("authentication.not_linked?"),
	AUTH_NOT_IN_SERVER("authentication.not_in_server?"),
	AUTH_ABSENT_ROLE("authentication.absent_role?"),
	MESSAGE_FORMAT_JOIN("message_format.join?"),
	MESSAGE_FORMAT_LEAVE("message_format.leave?"),
	MESSAGE_FORMAT_CHAT("message_format.chat?"),
	MESSAGE_FORMAT_DEATH("message_format.death?");

	public final String label;

	ConfigPath(final String label) {
		this.label = label;
	}
}
