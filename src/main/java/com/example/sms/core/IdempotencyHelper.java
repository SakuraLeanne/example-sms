package com.example.sms.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 幂等控制工具，用于防止短时间内重复发送。
 */
@Component
public class IdempotencyHelper {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    /**
     * 检查是否重复请求。
     */
    public void check(String scene, String mobile, Map<String, String> params, long windowSeconds) {
        if (windowSeconds <= 0) {
            return;
        }
        String key = buildKey(scene, mobile, params);
        Long expiresAt = cache.get(key);
        if (expiresAt != null && expiresAt > Instant.now().getEpochSecond()) {
            throw new SmsException(ErrorCode.IDEMPOTENT_REJECTED, "幂等窗口内重复请求");
        }
    }

    /**
     * 记录请求，超时自动失效。
     */
    public void mark(String scene, String mobile, Map<String, String> params, long windowSeconds) {
        if (windowSeconds <= 0) {
            return;
        }
        String key = buildKey(scene, mobile, params);
        cache.put(key, Instant.now().getEpochSecond() + windowSeconds);
        cleanup();
    }

    private String buildKey(String scene, String mobile, Map<String, String> params) {
        try {
            MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");
            digest.update(scene.getBytes(StandardCharsets.UTF_8));
            digest.update(mobile.getBytes(StandardCharsets.UTF_8));
            if (params != null) {
                params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                        // 不写入 value，避免敏感参数参与 hash。
                    });
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new SmsException(ErrorCode.CLIENT_ERROR, \"哈希算法不可用\", e);
        }
    }

    private void cleanup() {
        long now = Instant.now().getEpochSecond();
        cache.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
