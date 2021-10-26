package com.cykreet.arch.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cykreet.arch.Arch;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class LoggerUtil {
	private static Logger logger = Bukkit.getLogger();

	private static void log(Level level, String message) {
		logger.log(level, message);
	}

	public static void info(String message) {
		log(Level.INFO, message);
	}

	public static void warning(String message) {
		log(Level.WARNING, message);
	}
	
	public static void error(String message) {
		error(message, null);
	}
	
	public static void error(String message, @Nullable Exception exception) {
		String output = message;
		if (exception != null) output += "\n" + exception.getStackTrace().toString();
		log(Level.SEVERE, output);
	}
	
	public static void errorAndExit(String message) {
		errorAndExit(message, null);
	}

	public static void errorAndExit(String message, @Nullable Exception exception) {
		error(message, exception);
		Bukkit.getPluginManager().disablePlugin(Arch.getInstance());
	}
}
