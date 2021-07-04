package com.cykreet.arch.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cykreet.arch.Arch;

import org.bukkit.Bukkit;

public class LoggerUtil {
	private static Logger logger = Bukkit.getLogger();

	private static void log(String message, Level level) {
		logger.log(level, message);
	}

	public static void info(String message) {
		log(message, Level.INFO);
	}

	public static void warning(String message) {
		log(message, Level.WARNING);
	}

	public static void error(String message) {
		log(message, Level.SEVERE);
	}

	public static void errorAndExit(String message) {
		error(message);
		Bukkit.getPluginManager().disablePlugin(Arch.getInstance());
	}
}
