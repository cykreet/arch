package com.cykreet.arch.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.cykreet.arch.util.LoggerUtil;

import org.jetbrains.annotations.NotNull;

public class DatabaseManager extends Manager {
	private Connection connection;

	public void connect(@NotNull final File path, @NotNull final String name) {
		if (connection != null) return;
		File file = new File(path, name);
		String url = "jdbc:sqlite:" + file.getAbsolutePath();
		String sql = "CREATE TABLE IF NOT EXISTS linked_users (player_uuid BLOB PRIMARY KEY, discord_id TEXT NOT NULL);";
		try {
			Connection databaseConnection = DriverManager.getConnection(url);
			Statement statement = databaseConnection.createStatement();
			statement.execute(sql);
			statement.close();
			this.connection = databaseConnection;
		} catch (SQLException err) {
			LoggerUtil.errorAndExit("Failed to establish database connection.", err);
		}
	}

	public String getMemberId(@NotNull final UUID playerUUID) {
		String sql = String.format("SELECT * FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		String result;
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			result = resultSet.getString("discord_id");
			resultSet.close();
		} catch (SQLException err) {
			result = null;
		}

		return result;
	}

	public UUID getPlayerId(@NotNull final String discordId) {
		String sql = String.format("SELECT * FROM linked_users WHERE discord_id = \"%s\";", discordId);
		UUID result;
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			result = UUID.fromString(resultSet.getString("player_uuid"));
			resultSet.close();
		} catch (SQLException err) {
			result = null;
		}

		return result;
	}

	public boolean contains(@NotNull final String discordId) {
		String sql = String.format("SELECT * FROM linked_users WHERE discord_id = \"%s\";", discordId);
		boolean result;
		try (Statement statment = this.connection.createStatement()) {
			ResultSet resultSet = statment.executeQuery(sql);
			result = resultSet.next();
			resultSet.close();
		} catch (SQLException err) {
			result = false;
		}

		return result;
	}

	public boolean contains(@NotNull final UUID playerUUID) {
		String sql = String.format("SELECT * FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		boolean result;
		try (Statement statement = this.connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(sql);
			result = resultSet.next();
			resultSet.close();
		} catch (SQLException err) {
			result = false;
		}

		return result;
	}

	public boolean insert(@NotNull final UUID playerUUID, @NotNull final String discordId) {
		String sql = String.format(
			"INSERT INTO linked_users (player_uuid, discord_id) VALUES (\"%h\", \"%s\");",
			playerUUID,
			discordId
		);
		try (Statement statement = this.connection.createStatement()) {
			statement.executeQuery(sql);
			statement.close();
			return true;
		} catch (SQLException err) {
			LoggerUtil.error("Failed to insert user into database:\n", err);
			return false;
		}
	}

	public void removeByPlayerId(@NotNull final UUID playerUUID) {
		String sql = String.format("DELETE FROM linked_users WHERE player_uuid = \"%h\";", playerUUID);
		try (Statement statement = this.connection.createStatement()) {
			statement.executeUpdate(sql);
			statement.close();
		} catch (SQLException err) {
			LoggerUtil.error("Failed to remove user from database:\n", err);
		}
	}

	public void removeByMemberId(@NotNull final String discordId) {
		String sql = String.format("DELETE FROM linked_users WHERE discord_id = \"%s\";", discordId);
		try (Statement statement = this.connection.createStatement()) {
			statement.executeUpdate(sql);
			statement.close();
		} catch (SQLException err) {
			LoggerUtil.error("Failed to remove user from database:\n", err);
		}
	}

	public void close() {
		if (connection == null) return;
		try {
			if (connection.isClosed()) return;
			this.connection.close();
		} catch (SQLException err) {
			LoggerUtil.error("Failed to close database connection.", err);
		}
	}
}
