package com.cykreet.arch.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cykreet.arch.Arch;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class LoggerUtil {
	private static Logger logger = Bukkit.getLogger();

	private static void log(final Level level, final String message) {
		logger.log(level, message);
	}

	public static void info(final String message) {
		log(Level.INFO, message);
	}

	public static void warning(final String message) {
		log(Level.WARNING, message);
	}

	public static void error(final String message) {
		error(message, null);
	}

	public static void error(final String message, @Nullable final Exception exception) {
		String output = message;
		if (exception != null) output += "\n" + ExceptionUtils.getStackTrace(exception);
		log(Level.SEVERE, output);
	}

	public static void errorAndExit(final String message) {
		errorAndExit(message, null);
	}

	public static void errorAndExit(final String message, @Nullable final Exception exception) {
		error(message, exception);
		Bukkit.getPluginManager().disablePlugin(Arch.getInstance());
	}
}
