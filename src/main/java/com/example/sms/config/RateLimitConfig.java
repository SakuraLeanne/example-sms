package com.example.sms.config;

import javax.validation.constraints.Min;

/**
 * 短信限流配置。
 */
public class RateLimitConfig {

    /** 每个手机号发送间隔，单位秒 */
    @Min(0)
    private long perMobileIntervalSeconds;

    /** 每个手机号每日最大发送次数 */
    @Min(0)
    private long perMobileDayLimit;

    /** 每个 IP 每分钟最大发送次数 */
    @Min(0)
    private long perIpMinuteLimit;

    public long getPerMobileIntervalSeconds() {
        return perMobileIntervalSeconds;
    }

    public void setPerMobileIntervalSeconds(long perMobileIntervalSeconds) {
        this.perMobileIntervalSeconds = perMobileIntervalSeconds;
    }

    public long getPerMobileDayLimit() {
        return perMobileDayLimit;
    }

    public void setPerMobileDayLimit(long perMobileDayLimit) {
        this.perMobileDayLimit = perMobileDayLimit;
    }

    public long getPerIpMinuteLimit() {
        return perIpMinuteLimit;
    }

    public void setPerIpMinuteLimit(long perIpMinuteLimit) {
        this.perIpMinuteLimit = perIpMinuteLimit;
    }
}
