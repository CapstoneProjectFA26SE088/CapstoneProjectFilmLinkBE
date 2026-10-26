package com.fa26se088.filmlink.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình in-memory caching với TTL = 24h cho master data (MD-01, BR-16).
 * Master data rất ít khi thay đổi nhưng mật độ đọc cực cao; TTL 24h giúp giảm tải DB.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  public static final String CACHE_FILM_PROCESSES = "film_processes";
  public static final String CACHE_FILM_FORMATS = "film_formats";
  public static final String CACHE_SERVICE_TYPES = "service_types";
  public static final String CACHE_FILM_STOCKS = "film_stocks";
  public static final String CACHE_SYSTEM_PARAMETERS = "system_parameters";

  public static final Duration MASTERDATA_TTL = Duration.ofHours(24);

  @Bean
  public CacheManager cacheManager() {
    return new CacheManager() {
      private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

      @Override
      public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> new TtlConcurrentMapCache(n, MASTERDATA_TTL));
      }

      @Override
      public Collection<String> getCacheNames() {
        return caches.keySet();
      }
    };
  }

  private static final class TtlConcurrentMapCache implements Cache {

    private record CachedEntry(Object value, Instant expiresAt) {}

    private final String name;
    private final Duration ttl;
    private final ConcurrentMap<Object, CachedEntry> store = new ConcurrentHashMap<>();

    private TtlConcurrentMapCache(String name, Duration ttl) {
      this.name = name;
      this.ttl = ttl;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Object getNativeCache() {
      return store;
    }

    @Override
    public ValueWrapper get(Object key) {
      CachedEntry entry = store.get(key);
      if (entry == null) {
        return null;
      }
      if (Instant.now().isAfter(entry.expiresAt())) {
        store.remove(key, entry);
        return null;
      }
      return new SimpleValueWrapper(entry.value());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
      ValueWrapper wrapper = get(key);
      if (wrapper == null) {
        return null;
      }
      Object value = wrapper.get();
      if (value != null && !type.isInstance(value)) {
        throw new IllegalStateException(
            "Cached value is not of required type [" + type.getName() + "]: " + value);
      }
      return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
      ValueWrapper wrapper = get(key);
      if (wrapper != null) {
        return (T) wrapper.get();
      }
      synchronized (this) {
        wrapper = get(key);
        if (wrapper != null) {
          return (T) wrapper.get();
        }
        try {
          T value = valueLoader.call();
          put(key, value);
          return value;
        } catch (Exception ex) {
          throw new ValueRetrievalException(key, valueLoader, ex);
        }
      }
    }

    @Override
    public void put(Object key, Object value) {
      store.put(key, new CachedEntry(value, Instant.now().plus(ttl)));
    }

    @Override
    public void evict(Object key) {
      store.remove(key);
    }

    @Override
    public void clear() {
      store.clear();
    }
  }
}
