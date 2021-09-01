package com.cykreet.arch.managers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CacheManager extends Manager {
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
}
