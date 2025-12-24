package com.example.sms.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.exception.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.sms.config.SceneConfig;
import com.example.sms.config.SceneConfig.SceneType;
import com.example.sms.config.SmsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 阿里云短信客户端封装，支持直接注入为组件或通过配置类注入 Bean。
 */
@Component
public class AliyunSmsClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SmsProperties smsProperties;
    private final SceneConfigResolver sceneConfigResolver;
    private final RateLimitHelper rateLimitHelper;
    private final IdempotencyHelper idempotencyHelper;
    private final ProviderErrorMapper providerErrorMapper;
    private final AtomicReference<Client> cachedClient = new AtomicReference<>();

    public AliyunSmsClient(SmsProperties smsProperties,
                           SceneConfigResolver sceneConfigResolver,
                           RateLimitHelper rateLimitHelper,
                           IdempotencyHelper idempotencyHelper,
                           ProviderErrorMapper providerErrorMapper) {
        this.smsProperties = smsProperties;
        this.sceneConfigResolver = sceneConfigResolver;
        this.rateLimitHelper = rateLimitHelper;
        this.idempotencyHelper = idempotencyHelper;
        this.providerErrorMapper = providerErrorMapper;
    }

    /**
     * 发送短信（包含验证码/通知），入口只允许 scene 与手机号、参数。
     * 兼容旧接口，通过场景配置解析 sign 与模板。
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

        SendResult result = send(mobile, config.getSignName(), config.getTemplateCode(), castToObjectMap(safeParams), null);
        if (!result.isSuccess()) {
            throw new SmsException(result.getErrorCode() != null ? result.getErrorCode() : ErrorCode.CLIENT_ERROR,
                "阿里云短信发送失败：" + result.getProviderCode());
        }
        idempotencyHelper.mark(scene, mobile, safeParams, config.getIdempotencyWindowSeconds());
    }

    /**
     * 使用新版阿里云短信 SDK 发送短信。
     */
    public SendResult send(String mobile, String signName, String templateCode, Map<String, Object> params, String outId) {
        long start = System.currentTimeMillis();
        String maskedMobile = maskMobile(mobile);
        Set<String> paramKeys = CollectionUtils.isEmpty(params) ? Collections.emptySet() : params.keySet();
        try {
            SendSmsRequest request = buildRequest(mobile, signName, templateCode, params, outId);
            SendSmsResponse response = getClient().sendSmsWithOptions(request, buildRuntimeOptions());
            SendSmsResponseBody body = response.getBody();
            String providerCode = body == null ? null : body.getCode();
            boolean success = "OK".equalsIgnoreCase(providerCode);
            ErrorCode errorCode = success ? null : providerErrorMapper.mapCode(providerCode);
            long cost = System.currentTimeMillis() - start;
            log.info("Aliyun SMS send {} mobile={}, traceId={}, requestId={}, providerRequestId={}, code={}, cost={}ms, paramKeys={}",
                success ? "success" : "fail",
                maskedMobile,
                currentTraceId(),
                body == null ? "" : body.getRequestId(),
                body == null ? "" : body.getBizId(),
                providerCode,
                cost,
                paramKeys);
            return new SendResult(
                success,
                body == null ? null : body.getBizId(),
                providerCode,
                body == null ? null : body.getMessage(),
                body == null ? null : body.getRequestId(),
                errorCode);
        } catch (Exception ex) {
            ErrorCode mapped = providerErrorMapper.mapException(ex);
            long cost = System.currentTimeMillis() - start;
            log.warn("Aliyun SMS send exception mobile={}, traceId={}, cost={}ms, errorCode={}, paramKeys={}, err={}",
                maskedMobile,
                currentTraceId(),
                cost,
                mapped,
                paramKeys,
                ex.toString());
            throw new SmsException(mapped, "阿里云短信发送异常", ex);
        }
    }

    private Map<String, Object> castToObjectMap(Map<String, String> params) {
        if (CollectionUtils.isEmpty(params)) {
            return Collections.emptyMap();
        }
        return params.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private SendSmsRequest buildRequest(String mobile, String signName, String templateCode, Map<String, Object> params, String outId) {
        SendSmsRequest request = new SendSmsRequest()
            .setPhoneNumbers(mobile)
            .setSignName(signName)
            .setTemplateCode(templateCode)
            .setOutId(outId);
        if (!CollectionUtils.isEmpty(params)) {
            request.setTemplateParam(serializeParams(params));
        }
        return request;
    }

    private RuntimeOptions buildRuntimeOptions() {
        RuntimeOptions options = new RuntimeOptions();
        int timeout = smsProperties.getAliyun().getTimeout();
        options.setConnectTimeout(timeout);
        options.setReadTimeout(timeout);
        return options;
    }

    private Client getClient() throws Exception {
        Client existing = cachedClient.get();
        if (existing != null) {
            return existing;
        }
        if (!smsProperties.getAliyun().isComplete()) {
            throw new SmsException(ErrorCode.INVALID_CONFIG, "阿里云短信配置不完整");
        }
        Config config = new Config()
            .setAccessKeyId(smsProperties.getAliyun().getAccessKeyId())
            .setAccessKeySecret(smsProperties.getAliyun().getAccessKeySecret())
            .setEndpoint(smsProperties.getAliyun().getEndpoint())
            .setConnectTimeout(smsProperties.getAliyun().getTimeout())
            .setReadTimeout(smsProperties.getAliyun().getTimeout());
        Client created = new Client(config);
        cachedClient.compareAndSet(null, created);
        return cachedClient.get();
    }

    private String serializeParams(Map<String, Object> params) {
        try {
            return OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new SmsException(ErrorCode.CLIENT_ERROR, "模板参数序列化失败", e);
        }
    }

    private Map<String, String> filterParams(SceneConfig config, Map<String, String> params) {
        if (CollectionUtils.isEmpty(params)) {
            return Collections.emptyMap();
        }
        Map<String, String> filtered = new java.util.HashMap<>();
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

    private String maskMobile(String mobile) {
        if (StringUtils.length(mobile) < 7) {
            return "**" + StringUtils.right(mobile, 2);
        }
        return StringUtils.left(mobile, 3) + "****" + StringUtils.right(mobile, 2);
    }

    private String currentTraceId() {
        return Objects.toString(MDC.get("traceId"), "");
    }
}
