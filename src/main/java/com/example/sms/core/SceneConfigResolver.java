package com.example.sms.core;

import org.springframework.stereotype.Component;

import com.example.sms.config.SceneConfig;
import com.example.sms.config.SmsProperties;

/**
 * 场景配置解析工具，所有外部调用必须先通过 scene 查找配置。
 */
@Component
public class SceneConfigResolver {

    private final SmsProperties smsProperties;

    public SceneConfigResolver(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    /**
     * 根据 scene 获取配置，未找到抛出业务异常。
     * @param scene 场景编码
     * @return 场景配置
     */
    public SceneConfig getRequired(String scene) {
        SceneConfig config = smsProperties.getScenes().get(scene);
        if (config == null) {
            throw new SmsException(ErrorCode.SCENE_NOT_FOUND, "未找到短信场景:" + scene);
        }
        return config;
    }
}
