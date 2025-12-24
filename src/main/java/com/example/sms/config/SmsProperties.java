package com.example.sms.config;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.example.sms.core.ErrorCode;
import com.example.sms.core.SmsException;

/**
 * 短信配置绑定，包含阿里云凭证与场景治理。
 */
@Validated
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 阿里云基础配置 */
    private final AliyunProperties aliyun = new AliyunProperties();

    /** scene -> 配置映射 */
    @NotEmpty
    @Valid
    private Map<String, SceneConfig> scenes;

    @PostConstruct
    public void validate() {
        if (scenes == null || scenes.isEmpty()) {
            throw new SmsException(ErrorCode.INVALID_CONFIG, "短信场景配置不能为空");
        }
        scenes.forEach(this::validateScene);
    }

    private void validateScene(String scene, SceneConfig config) {
        if (config == null) {
            throw new SmsException(ErrorCode.INVALID_CONFIG, "场景" + scene + "配置缺失");
        }
        if (config.getParamWhitelist() == null || config.getParamWhitelist().isEmpty()) {
            throw new SmsException(ErrorCode.INVALID_CONFIG, "场景" + scene + "参数白名单不能为空");
        }
        if (config.getType() == SceneConfig.SceneType.VERIFY_CODE) {
            if (config.getExpireSeconds() <= 0 || StringUtils.isBlank(config.getCodeParamKey())) {
                throw new SmsException(ErrorCode.INVALID_CONFIG, "验证码场景" + scene + "必须配置过期时间与验证码参数名");
            }
            if (!config.getParamWhitelist().contains(config.getCodeParamKey())) {
                throw new SmsException(ErrorCode.INVALID_CONFIG, "验证码场景" + scene + "白名单必须包含验证码参数名");
            }
        }
    }

    public AliyunProperties getAliyun() {
        return aliyun;
    }

    public Map<String, SceneConfig> getScenes() {
        return scenes;
    }

    public void setScenes(Map<String, SceneConfig> scenes) {
        this.scenes = scenes;
    }

    /**
     * 阿里云短信账户配置。
     */
    public static class AliyunProperties {

        private String accessKeyId;

        private String accessKeySecret;

        private String endpoint;

        private int timeout = 5000;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public boolean isComplete() {
            return StringUtils.isNotBlank(accessKeyId)
                && StringUtils.isNotBlank(accessKeySecret)
                && StringUtils.isNotBlank(endpoint);
        }

        @Override
        public String toString() {
            return "AliyunProperties{endpoint='" + endpoint + '\'' + "'}";
        }
    }
}
