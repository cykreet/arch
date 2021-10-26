package com.cykreet.arch.managers;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CodesManager extends Manager {
	private final char[] characters = "0213546879".toCharArray();
	private Cache<UUID, String> cache;

	public void createCache(int expires) {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(expires, TimeUnit.MINUTES)
			.maximumSize(100)
			.build();
	}

	public Cache<UUID, String> getCache() {
		return this.cache;
	}

	public String generateRandomCode() {
		char buffer[] = new char[4];
		Random random = new Random();
		for (int i = 0; i < buffer.length; i++) {
			int index = random.nextInt(characters.length);
			buffer[i] = characters[index];
		}

		return new String(buffer);
	}
}