package com.example.sms.core;

/**
 * 短信发送结果。
 */
public class SendResult {

    private final boolean success;
    private final String providerRequestId;
    private final String providerCode;
    private final String providerMessage;
    private final String requestId;
    private final ErrorCode errorCode;

    public SendResult(boolean success, String providerRequestId, String providerCode, String providerMessage, String requestId, ErrorCode errorCode) {
        this.success = success;
        this.providerRequestId = providerRequestId;
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
