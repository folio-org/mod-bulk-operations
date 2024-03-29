package org.folio.bulkops.configs;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public Caffeine caffeineConfig() { //NOSONAR
    return Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS);
  }

  @Bean
  public CacheManager cacheManager(Caffeine caffeine) { //NOSONAR
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(caffeine);
    return caffeineCacheManager;
  }
}
