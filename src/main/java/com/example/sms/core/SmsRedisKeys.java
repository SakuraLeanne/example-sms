package com.example.sms.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 短信 Redis key 规范。
 */
public final class SmsRedisKeys {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private SmsRedisKeys() {
    }

    public static String smsCode(String scene, String mobile) {
        return String.format("sms:code:%s:%s", scene, mobile);
    }

    public static String limitMobileDay(String scene, String mobile, LocalDate date) {
        return String.format("sms:limit:mobile:day:%s:%s:%s", scene, mobile, DATE_FMT.format(date));
    }

    public static String limitIpMinute(String ip, LocalDateTime minuteWindow) {
        return String.format("sms:limit:ip:minute:%s:%s", ip, MINUTE_FMT.format(minuteWindow));
    }

    public static String lock(String scene, String mobile) {
        return String.format("sms:lock:%s:%s", scene, mobile);
    }

    public static String idempotent(String scene, String bizId) {
        return String.format("sms:idem:%s:%s", scene, bizId);
    }
}
