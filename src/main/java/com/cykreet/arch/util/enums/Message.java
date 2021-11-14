package com.cykreet.arch.util.enums;

public enum Message {
	DISCORD_INVALID_CODE(
		"Invalid code provided, please make sure the code you're trying to use hasn't expired and try again."
		),
	DISCORD_ALREADY_LINKED_ACCOUNT(
		"Your Discord account has already been linked to `%s`."
		+ " If you'd like to unlink your account, reply with `unlink`."
	),
	DISCORD_LINKED_ACCOUNT("Your Discord account has been linked to `%s`."),
	DISCORD_UNLINKED_ACCOUNT("Your Discord account has been unlinked from `%s`.");

	public final String content;

	Message(final String content) {
		this.content = content;
	}
}
