package com.example.sms.core;

/**
 * 短信错误码定义。
 */
public enum ErrorCode {

    /** 配置错误 */
    INVALID_CONFIG,

    /** 场景不存在 */
    SCENE_NOT_FOUND,

    /** 发送被限流 */
    RATE_LIMITED,

    /** 幂等窗口内重复请求 */
    IDEMPOTENT_REJECTED,

    /** 客户端内部错误 */
    CLIENT_ERROR
}
