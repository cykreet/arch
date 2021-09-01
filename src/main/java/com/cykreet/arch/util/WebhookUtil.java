package com.cykreet.arch.util;

import java.io.OutputStream;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.jetbrains.annotations.NotNull;

public class WebhookUtil {
	public static void post(@NotNull URL url, @NotNull String body) throws Exception {
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; utf-8");
		connection.setConnectTimeout(12 * 1000);
		connection.setDoOutput(true);
		connection.connect();

		OutputStream os = connection.getOutputStream();
		byte[] input = body.getBytes("utf-8");
		os.write(input, 0, input.length);
		os.flush();
		os.close();

		if (connection.getResponseCode() > 400) throw new Exception(connection.getResponseMessage());
	}

	public static String getPlayerAvatar(@NotNull UUID playerUUId) {
		return String.format("https://crafatar.com/avatars/%s.png?size=120&overlay", playerUUId.toString());
	}
}
