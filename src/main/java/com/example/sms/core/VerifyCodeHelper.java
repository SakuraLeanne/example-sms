package com.example.sms.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.example.sms.config.SceneConfig;
import com.example.sms.config.SceneConfig.SceneType;

/**
 * 验证码发送与存储辅助工具。
 * 注意：不记录任何验证码明文或哈希日志，调用方仅可存储哈希。
 */
@Component
public class VerifyCodeHelper {

    private final AliyunSmsClient aliyunSmsClient;
    private final SceneConfigResolver sceneConfigResolver;

    public VerifyCodeHelper(AliyunSmsClient aliyunSmsClient, SceneConfigResolver sceneConfigResolver) {
        this.aliyunSmsClient = aliyunSmsClient;
        this.sceneConfigResolver = sceneConfigResolver;
    }

    /**
     * 发送验证码短信，场景必须为 VERIFY_CODE。
     * @param scene 场景编码
     * @param mobile 手机号
     * @param code 验证码明文，内部仅用于发送，不做任何日志输出
     * @param clientIp 客户端 IP
     */
    public void sendVerifyCode(String scene, String mobile, String code, String clientIp) {
        SceneConfig config = sceneConfigResolver.getRequired(scene);
        Assert.isTrue(config.getType() == SceneType.VERIFY_CODE, \"场景类型必须为验证码\");
        Map<String, String> params = new HashMap<>();
        params.put(config.getCodeParamKey(), code);
        aliyunSmsClient.send(scene, mobile, params, clientIp);
    }

    /**
     * 计算验证码哈希，供业务层存储校验。
     * 返回值仅用于存储，不应写入日志或透传。
     */
    public String hashCode(String code, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");
            digest.update(safeBytes(code));
            digest.update(safeBytes(salt));
            byte[] hashed = digest.digest();
            // 不返回明文，仅返回哈希
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new SmsException(ErrorCode.CLIENT_ERROR, \"哈希算法不可用\", e);
        }
    }

    private byte[] safeBytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(\"%02x\", b));
        }
        return sb.toString();
    }
}
