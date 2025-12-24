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
    CLIENT_ERROR,

    /** 服务商限流 */
    PROVIDER_THROTTLED,

    /** 服务商参数错误 */
    PROVIDER_INVALID_PARAM,

    /** 服务商认证失败 */
    PROVIDER_AUTH_ERROR,

    /** 服务商未知错误 */
    PROVIDER_ERROR
}
