package com.example.sms.core;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 短信 Redis 仓储封装。
 */
@Component
public class RedisSmsRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSmsRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Map<Object, Object> getHash(String key) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        return CollectionUtils.isEmpty(entries) ? Collections.emptyMap() : entries;
    }

    public void setHash(String key, Map<String, String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        stringRedisTemplate.opsForHash().putAll(key, values);
    }

    public Boolean expire(String key, long ttlSeconds) {
        return stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    public void del(String key) {
        stringRedisTemplate.delete(key);
    }

    public long incrWithTtl(String key, long ttlSeconds) {
        Long value = stringRedisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        return value == null ? 0L : value;
    }

    public boolean setIfAbsent(String key, String value, long ttlSeconds) throws DataAccessException {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }
}
