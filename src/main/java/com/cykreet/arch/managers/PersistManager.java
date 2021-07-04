package com.cykreet.arch.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.cykreet.arch.Arch;
import com.cykreet.arch.util.LoggerUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PersistManager {
	private Connection connection;
	private File file;

	public PersistManager(@NotNull File path, @NotNull String name) {
		this.file = new File(path, name);
	}
	
	public void connect() {
		if (connection != null) return; 
		String url = "jdbc:sqlite:" + this.file.getAbsolutePath();
		String sql = "CREATE TABLE IF NOT EXISTS linked_users (player_uuid BLOB PRIMARY KEY, discord_id TEXT NOT NULL);";
		try {
			Connection connection = DriverManager.getConnection(url);
			Statement statement = connection.createStatement();
			statement.execute(sql);
			this.connection = connection;
		} catch (SQLException e) {
			LoggerUtil.errorAndExit("Failed to establish database connection.");
		}

		return;
	}

	public String getMemberId(UUID playerUUID) {
		String sql = String.format("SELECT * FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			return resultSet.getString("discord_id");
		} catch (Exception e) {
			return null;
		}
	}

	public UUID getPlayerId(String discordId) {
		String sql = String.format("SELECT * FROM linked_users WHERE discord_id = \"%s\";", discordId);
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			return UUID.fromString(resultSet.getString("player_uuid"));
		} catch (Exception e) {
			return null;
		}
	}

	public boolean contains(String discordId) {
		String sql = String.format("SELECT * FROM linked_users WHERE discord_id = \"%s\";", discordId);
		try (Statement statment = this.connection.createStatement()) {
			ResultSet ResultSet = statment.executeQuery(sql);
			return ResultSet.next();
		} catch (SQLException e) {
			return false;
		}
	}

	public boolean contains(UUID playerUUID) {
		String sql = String.format("SELECT * FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			return resultSet.next();
		} catch (SQLException e) {
			return false;
		}
	}

	public boolean insert(UUID playerUUID, String discordId) {
		String sql = String.format(
			"INSERT INTO linked_users (player_uuid, discord_id) VALUES (\"%h\", \"%s\");", 
			playerUUID, 
			discordId
		);
		try (Statement statement = this.connection.createStatement()) {
			statement.execute(sql);
			return true;
		} catch (SQLException e) {
			LoggerUtil.error("Failed to insert user into database:\n" + e.getStackTrace());
			return false;
		}
	}

	public void removeByPlayerId(UUID playerUUID) {
		String sql = String.format("DELETE FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		try (Statement statement = this.connection.createStatement()) {
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LoggerUtil.error("Failed to remove user from database: \n" + e.getStackTrace());
		}
	}

	public void removeByMemberId(String discordId) {
		String sql = String.format("DELETE FROM linked_users WHERE discord_id = \"%s\";", discordId);
		try (Statement statement = this.connection.createStatement()) {
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LoggerUtil.error("Failed to remove user from database: \n" + e.getStackTrace());
		}
	}

	public void close() {
		if (connection == null) return;
		try {
			if (connection.isClosed()) return;
			this.connection.close();
		} catch (SQLException e) {
			LoggerUtil.error("Failed to close database connection.");
		}

		return;
	}

	public void unlinkPlayer(@NotNull UUID playerId) {
		this.removeByPlayerId(playerId);
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
		if (!offlinePlayer.isOnline()) return;
		Bukkit.getScheduler().runTaskAsynchronously(Arch.getInstance(), () -> {
			offlinePlayer.getPlayer().kickPlayer("Unlinked Discord account.");
		});
		
		return;
	}
}
