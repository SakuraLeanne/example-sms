package com.example.sms.config;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 * 场景配置，描述签名、模板等信息，只能通过 scene 使用。
 */
public class SceneConfig {

    /** 短信服务商，目前仅支持阿里云 */
    public enum Provider {
        ALIYUN
    }

    /** 场景类型：验证码或通知 */
    public enum SceneType {
        VERIFY_CODE,
        NOTIFY
    }

    @NotBlank
    private String signName;

    @NotBlank
    private String templateCode;

    private Provider provider = Provider.ALIYUN;

    private SceneType type = SceneType.NOTIFY;

    @Min(0)
    private long expireSeconds;

    private String codeParamKey;

    @NotEmpty
    private List<String> paramWhitelist;

    @Valid
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Min(0)
    private long idempotencyWindowSeconds;

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public SceneType getType() {
        return type;
    }

    public void setType(SceneType type) {
        this.type = type;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getCodeParamKey() {
        return codeParamKey;
    }

    public void setCodeParamKey(String codeParamKey) {
        this.codeParamKey = codeParamKey;
    }

    public List<String> getParamWhitelist() {
        return paramWhitelist;
    }

    public void setParamWhitelist(List<String> paramWhitelist) {
        this.paramWhitelist = paramWhitelist;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public long getIdempotencyWindowSeconds() {
        return idempotencyWindowSeconds;
    }

    public void setIdempotencyWindowSeconds(long idempotencyWindowSeconds) {
        this.idempotencyWindowSeconds = idempotencyWindowSeconds;
    }
}
