package com.example.sms.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.example.sms.config.SceneConfig;
import com.example.sms.config.SceneConfig.SceneType;
import com.example.sms.config.SmsProperties;

/**
 * 阿里云短信客户端封装，严格通过 scene 驱动，不接受 sign/template 入参。
 */
@Component
public class AliyunSmsClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsClient.class);

    private final SmsProperties smsProperties;
    private final SceneConfigResolver sceneConfigResolver;
    private final RateLimitHelper rateLimitHelper;
    private final IdempotencyHelper idempotencyHelper;

    public AliyunSmsClient(SmsProperties smsProperties,
                           SceneConfigResolver sceneConfigResolver,
                           RateLimitHelper rateLimitHelper,
                           IdempotencyHelper idempotencyHelper) {
        this.smsProperties = smsProperties;
        this.sceneConfigResolver = sceneConfigResolver;
        this.rateLimitHelper = rateLimitHelper;
        this.idempotencyHelper = idempotencyHelper;
    }

    /**
     * 发送短信（包含验证码/通知），入口只允许 scene 与手机号、参数。
     * @param scene 场景编码
     * @param mobile 手机号
     * @param params 模板参数，必须经过白名单过滤
     * @param clientIp 客户端 IP，用于限流
     */
    public void send(String scene, String mobile, Map<String, String> params, String clientIp) {
        SceneConfig config = sceneConfigResolver.getRequired(scene);
        Map<String, String> safeParams = filterParams(config, params);
        rateLimitHelper.checkAndRecord(scene, mobile, clientIp, config);
        idempotencyHelper.check(scene, mobile, safeParams, config.getIdempotencyWindowSeconds());

        // 真实发送实现可在此替换为阿里云 SDK 调用，此处仅构造必要参数。
        Map<String, Object> requestPayload = buildPayload(config, mobile, safeParams);
        log.info("准备发送短信，scene={}，mobile={}，paramKeys={}", scene, mobile, safeParams.keySet());

        doSend(requestPayload);

        idempotencyHelper.mark(scene, mobile, safeParams, config.getIdempotencyWindowSeconds());
    }

    /**
     * 调用阿里云发送短信；这里留作对接阿里云 SDK 的钩子。
     * 为满足“通道层日志不得输出模板参数原始值”要求，日志仅输出参数 key 集合。
     */
    protected void doSend(Map<String, Object> requestPayload) {
        if (log.isDebugEnabled()) {
            Set<String> paramKeys = ((Map<String, ?>) requestPayload.getOrDefault("params", Collections.emptyMap())).keySet();
            log.debug("短信请求构造完成，endpoint={}，templateCode={}，paramKeys={}",
                smsProperties.getAliyun().getEndpoint(),
                requestPayload.get("templateCode"),
                paramKeys);
        }
        // 此处可扩展实际 HTTP / SDK 调用
    }

    private Map<String, String> filterParams(SceneConfig config, Map<String, String> params) {
        if (CollectionUtils.isEmpty(params)) {
            return Collections.emptyMap();
        }
        Map<String, String> filtered = new HashMap<>();
        for (String key : config.getParamWhitelist()) {
            if (params.containsKey(key)) {
                filtered.put(key, params.get(key));
            }
        }
        if (config.getType() == SceneType.VERIFY_CODE && !filtered.containsKey(config.getCodeParamKey())) {
            throw new SmsException(ErrorCode.INVALID_CONFIG, "验证码场景缺少必要参数");
        }
        return filtered;
    }

    private Map<String, Object> buildPayload(SceneConfig config, String mobile, Map<String, String> safeParams) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("provider", config.getProvider());
        payload.put("signName", config.getSignName());
        payload.put("templateCode", config.getTemplateCode());
        payload.put("mobile", mobile);
        payload.put("params", safeParams);
        return payload;
    }
}
