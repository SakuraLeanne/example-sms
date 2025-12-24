package com.example.sms.core;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.sms.config.RateLimitConfig;
import com.example.sms.config.SceneConfig;

/**
 * 简单限流工具，基于内存计数实现，可替换为 Redis 实现。
 */
@Component
public class RateLimitHelper {

    private static final Logger log = LoggerFactory.getLogger(RateLimitHelper.class);

    /** 场景+手机号 -> 上次发送时间戳 */
    private final Map<String, Long> mobileIntervalCache = new ConcurrentHashMap<>();
    /** 场景+手机号+日期 -> 当日计数 */
    private final Map<String, AtomicLong> mobileDayCounter = new ConcurrentHashMap<>();
    /** 场景+IP+分钟窗口 -> 计数 */
    private final Map<String, AtomicLong> ipMinuteCounter = new ConcurrentHashMap<>();

    /**
     * 校验并记录限流信息。
     */
    public void checkAndRecord(String scene, String mobile, String clientIp, SceneConfig config) {
        RateLimitConfig limit = config.getRateLimit();
        long now = Instant.now().getEpochSecond();
        checkInterval(scene, mobile, limit, now);
        checkMobileDaily(scene, mobile, limit, now);
        checkIpMinute(scene, clientIp, limit, now);
        // 记录发送行为
        mobileIntervalCache.put(buildMobileKey(scene, mobile), now);
    }

    private void checkInterval(String scene, String mobile, RateLimitConfig limit, long now) {
        long interval = limit.getPerMobileIntervalSeconds();
        if (interval <= 0) {
            return;
        }
        Long last = mobileIntervalCache.get(buildMobileKey(scene, mobile));
        if (last != null && now - last < interval) {
            throw new SmsException(ErrorCode.RATE_LIMITED, "发送过于频繁，请稍后再试");
        }
    }

    private void checkMobileDaily(String scene, String mobile, RateLimitConfig limit, long now) {
        long dayLimit = limit.getPerMobileDayLimit();
        if (dayLimit <= 0) {
            return;
        }
        String key = buildMobileDayKey(scene, mobile);
        AtomicLong counter = mobileDayCounter.computeIfAbsent(key, k -> new AtomicLong());
        long count = counter.incrementAndGet();
        if (count > dayLimit) {
            throw new SmsException(ErrorCode.RATE_LIMITED, "当日发送次数已达上限");
        }
        // 简易清理：日期变化时重置
        String today = LocalDate.now().toString();
        if (!key.endsWith(today)) {
            mobileDayCounter.remove(key);
        }
    }

    private void checkIpMinute(String scene, String clientIp, RateLimitConfig limit, long now) {
        long ipLimit = limit.getPerIpMinuteLimit();
        if (ipLimit <= 0 || !StringUtils.hasText(clientIp)) {
            return;
        }
        String key = buildIpKey(scene, clientIp, now / 60);
        AtomicLong counter = ipMinuteCounter.computeIfAbsent(key, k -> new AtomicLong());
        long count = counter.incrementAndGet();
        if (count > ipLimit) {
            log.warn("IP 限流触发，scene={}, ip={}", scene, clientIp);
            throw new SmsException(ErrorCode.RATE_LIMITED, "发送频率受限");
        }
        // 旧窗口清理
        ipMinuteCounter.keySet().removeIf(k -> {
            String[] parts = k.split("#");
            if (parts.length < 3) {
                return true;
            }
            long window = Long.parseLong(parts[2]);
            return now / 60 - window > 1;
        });
    }

    private String buildMobileKey(String scene, String mobile) {
        return scene + "#" + mobile;
    }

    private String buildMobileDayKey(String scene, String mobile) {
        return scene + "#" + mobile + "#" + LocalDate.now();
    }

    private String buildIpKey(String scene, String clientIp, long minuteWindow) {
        return scene + "#" + clientIp + "#" + minuteWindow;
    }
}
