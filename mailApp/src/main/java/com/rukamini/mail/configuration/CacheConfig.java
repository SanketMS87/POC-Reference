package com.rukamini.mail.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache infrastructure for MS Graph user lookups.
 *
 * <p><b>Named caches:</b>
 * <ul>
 *   <li>{@code graphUsers} — 10 min TTL, max 500 entries, stats recorded.
 *       Populated by {@link com.rukamini.mail.user.GraphUserDirectoryAdapter#lookupByUserId}
 *       when {@code app.user-directory.provider=graph}; keyed by user-ID string.
 *       {@code null} results are never cached so transient Graph failures are
 *       retried on the next invocation.
 * </ul>
 *
 * <p>When {@code app.user-directory.provider=idam} (the default) the
 * {@code graphUsers} cache is declared but never populated; Caffeine adds
 * negligible overhead in that mode.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager graphCacheManager() {
        CaffeineCache userCache =
                new CaffeineCache(
                        "graphUsers",
                        Caffeine.newBuilder()
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .maximumSize(500)
                                .recordStats()
                                .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(userCache));
        return manager;
    }
}
