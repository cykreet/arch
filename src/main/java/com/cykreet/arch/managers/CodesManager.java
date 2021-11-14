package com.cykreet.arch.managers;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CodesManager extends Manager {
	private static final int MAX_CACHE_ENTRIES = 100;
	private static final int CODE_LENGTH = 4;
	private final char[] characters = "0213546879".toCharArray();
	private Cache<UUID, String> cache;

	public void createCache(final int expires) {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(expires, TimeUnit.MINUTES)
			.maximumSize(CodesManager.MAX_CACHE_ENTRIES)
			.build();
	}

	public Cache<UUID, String> getCache() {
		return this.cache;
	}

	public String generateRandomCode() {
		char[] buffer = new char[CodesManager.CODE_LENGTH];
		Random random = new Random();
		for (int i = 0; i < buffer.length; i++) {
			int index = random.nextInt(characters.length);
			buffer[i] = characters[index];
		}

		return new String(buffer);
	}
}
