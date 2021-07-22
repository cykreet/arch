package com.cykreet.arch.util;

public enum Message {
	INTERNAL_BOT_NOT_IN_SERVER("Discord bot is not in the configured server, please invite the bot through the following link:\n%s"),
	INTERNAL_REQUIRED_CONFIG_PATH("Required config path \"%s\" is invalid or missing."),
	INTERNAL_AUTHENTICATION_DISABLED("\"%s\" config option not provided, disabling authentication..."),
	INTERNAL_BOT_REMOVED("Discord bot has forcefully been removed from the configured guild."),
	DISCORD_BOT_READY("Discord bot \"%s\", is ready."),
	DISCORD_INVALID_CODE("Invalid code provided, please make sure the code you're trying to use hasn't expired and try again."),
	DISCORD_LINKED_ACCOUNT("Your Discord account has been linked to `%s`."),
	DISCORD_UNLINKED_ACCOUNT("Your Discord account has been unlinked from `%s`."),
	DISCORD_ALREADY_LINKED_ACCOUNT("Your Discord account has already been linked to `%s`. If you'd like to unlink your account, reply with `unlink`.");

	public final String content;

	private Message(String content) {
		this.content = content;
	}
}
