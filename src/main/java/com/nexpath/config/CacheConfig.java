package com.nexpath.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 🛠️ CACHE CONFIGURATION
 * Replaces Redis with a simple in-memory ConcurrentMapCacheManager.
 * This satisfies any dependencies on 'cacheManager' without requiring a Redis server.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("user_credits", "career_roadmaps", "skill_gaps");
    }
}
