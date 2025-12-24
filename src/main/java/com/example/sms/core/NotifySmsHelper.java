package com.example.sms.core;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.example.sms.config.SceneConfig;
import com.example.sms.config.SceneConfig.SceneType;

/**
 * 通知短信发送工具，只允许通过场景驱动。
 */
@Component
public class NotifySmsHelper {

    private final AliyunSmsClient aliyunSmsClient;
    private final SceneConfigResolver sceneConfigResolver;

    public NotifySmsHelper(AliyunSmsClient aliyunSmsClient, SceneConfigResolver sceneConfigResolver) {
        this.aliyunSmsClient = aliyunSmsClient;
        this.sceneConfigResolver = sceneConfigResolver;
    }

    /**
     * 发送通知类短信，禁止外部传入 sign/template。
     * @param scene 场景编码
     * @param mobile 手机号
     * @param params 模板参数，仅取白名单字段
     * @param clientIp 客户端 IP
     */
    public void sendNotify(String scene, String mobile, Map<String, String> params, String clientIp) {
        SceneConfig config = sceneConfigResolver.getRequired(scene);
        Assert.isTrue(config.getType() == SceneType.NOTIFY, \"场景类型必须为通知\");
        aliyunSmsClient.send(scene, mobile, params, clientIp);
    }
}
